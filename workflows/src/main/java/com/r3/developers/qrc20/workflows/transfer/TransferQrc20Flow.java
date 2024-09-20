package com.r3.developers.qrc20.workflows.transfer;

import com.r3.developers.qrc20.contracts.Qrc20Commands;
import com.r3.developers.qrc20.states.Qrc20State;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatingFlow;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.TransactionState;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("DuplicatedCode")
@InitiatingFlow(protocol = "transfer-qrc20")
public class TransferQrc20Flow implements ClientStartableFlow {

    @CordaInject
    public FlowMessaging flowMessaging;

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public MemberLookup memberLookup;

    @CordaInject
    NotaryLookup notaryLookup;

    @CordaInject
    UtxoLedgerService utxoLedgerService;

    public TransferQrc20Flow() {
    }

    @Suspendable
    @Override
    @NotNull
    public String call(@NotNull final ClientRequestBody requestBody) {
        final var request = requestBody.getRequestBodyAs(jsonMarshallingService, TransferQrc20Request.class);
        final var holderFromName = request.getHolderFrom();
        final var holderToName = request.getHolderTo();

        final var callerKey = memberLookup.myInfo().getLedgerKeys().get(0);
        final var holderFromKey = Objects.requireNonNull(memberLookup.lookup(holderFromName))
                .getLedgerKeys()
                .get(0);
        if (callerKey != holderFromKey) {
            throw new IllegalArgumentException("Only holder can transfer");
        }

        // normally paging and filtering would be more complex, keeping it simple for POC
        final var inputStateAndRefs = utxoLedgerService.findUnconsumedStatesByExactType(
                        Qrc20State.class, 10000, Instant.now())
                .getResults()
                .stream()
                .filter(qrc20StateStateAndRef -> qrc20StateStateAndRef.getState()
                        .getContractState()
                        .getHolder()
                        .equals(callerKey))
                .filter(qrc20StateStateAndRef -> qrc20StateStateAndRef.getState()
                        .getContractState()
                        .getSymbol()
                        .equals(request.getSymbol()))
                .toList();
        final var transactionStates = inputStateAndRefs
                .stream()
                .map(StateAndRef::getState)
                .toList();
        final var ownerSupply = transactionStates.stream()
                .map(TransactionState::getContractState)
                .map(Qrc20State::getSupply)
                .reduce(Integer::sum)
                .orElse(0);

        final var notaryInfo = notaryLookup.getNotaryServices()
                .stream()
                .findFirst()
                .orElseThrow(RuntimeException::new);

        final var holderToMemberInfo = memberLookup.lookup(holderToName);
        if (holderToMemberInfo == null) {
            throw new IllegalArgumentException(
                    String.format("The holder %s does not exist within the network", holderToName));
        }
        final PublicKey holderToKey;
        try {
            holderToKey = holderToMemberInfo.getLedgerKeys().get(0);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("The holder %s has no ledger key", holderToMemberInfo));
        }

        if (request.getAmount() > ownerSupply) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        final var issuerKey = transactionStates.get(0).getContractState().getIssuer();
        final var change = ownerSupply - request.getAmount();
        final var output1 = new Qrc20State(
                UUID.randomUUID(),
                issuerKey,
                holderToKey,
                request.getSymbol(),
                request.getAmount(),
                List.of(callerKey, holderToKey)
        );
        final var output2 = new Qrc20State(
                UUID.randomUUID(),
                issuerKey,
                callerKey,
                request.getSymbol(),
                change,
                List.of(callerKey, callerKey)
        );

        final var transaction = utxoLedgerService.createTransactionBuilder()
                .setNotary(notaryInfo.getName())
                .addInputStates(inputStateAndRefs
                        .stream()
                        .map(StateAndRef::getRef)
                        .collect(Collectors.toList()))
                .addOutputStates(output1, output2)
                .addCommand(new Qrc20Commands.Transfer())
                .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
                .addSignatories(List.of(callerKey, holderToKey))
                .toSignedTransaction();

        final var session1 = flowMessaging.initiateFlow(holderFromName);
        final var session2 = flowMessaging.initiateFlow(request.getHolderTo());

        try {
            utxoLedgerService.finalize(transaction, List.of(session1, session2))
                    .getTransaction().getId();
            return String.format("Owner had %s, transferred %s and got change back %s",
                    ownerSupply, request.getAmount(), change);
        } catch (Exception e) {
            return String.format("Flow failed, message: %s", e.getMessage());
        }
    }
}
