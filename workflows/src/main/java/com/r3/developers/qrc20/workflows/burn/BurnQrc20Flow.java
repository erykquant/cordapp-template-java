package com.r3.developers.qrc20.workflows.burn;

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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"DuplicatedCode", "unused"})
@InitiatingFlow(protocol = "burn-qrc20")
public class BurnQrc20Flow implements ClientStartableFlow {

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

    public BurnQrc20Flow() {
    }

    @Suspendable
    @Override
    @NotNull
    public String call(@NotNull final ClientRequestBody requestBody) {
        final var request = requestBody.getRequestBodyAs(jsonMarshallingService, BurnQrc20Request.class);
        final var holderName = request.getHolder();

        final var callerKey = memberLookup.myInfo().getLedgerKeys().get(0);
        final var holderKey = Objects.requireNonNull(memberLookup.lookup(holderName))
                .getLedgerKeys()
                .get(0);
        if (callerKey != holderKey) {
            throw new IllegalArgumentException("Only holder can burn");
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
        final var transactionStates =
                inputStateAndRefs.stream()
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

        if (request.getAmount() > ownerSupply) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        final var amountLeft = ownerSupply - request.getAmount();

        final var output = new Qrc20State(
                UUID.randomUUID(),
                transactionStates.get(0).getContractState().getIssuer(),
                callerKey,
                request.getSymbol(),
                amountLeft,
                List.of(callerKey, callerKey));

        final var transaction = utxoLedgerService.createTransactionBuilder()
                .setNotary(notaryInfo.getName())
                .addInputStates(inputStateAndRefs
                        .stream()
                        .map(StateAndRef::getRef)
                        .collect(Collectors.toList()))
                .addOutputStates(output)
                .addCommand(new Qrc20Commands.Burn())
                .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
                .addSignatories(List.of(callerKey, callerKey))
                .toSignedTransaction();

        final var session = flowMessaging.initiateFlow(holderName);

        try {
            utxoLedgerService.finalize(transaction, List.of(session))
                    .getTransaction().getId();
            return String.format("Owner had %s, burned %s and got %s left",
                    ownerSupply, request.getAmount(), amountLeft);
        } catch (Exception e) {
            return String.format("Flow failed, message: %s", e.getMessage());
        }
    }
}
