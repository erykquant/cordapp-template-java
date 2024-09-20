package com.r3.developers.apples.workflows;

import com.r3.developers.apples.contracts.Qrc20Commands;
import com.r3.developers.apples.states.Qrc20State;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatingFlow;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowMessaging;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.TransactionState;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public String call(@NotNull ClientRequestBody requestBody) {
        TransferQrc20Request request = requestBody.getRequestBodyAs(jsonMarshallingService, TransferQrc20Request.class);
        var holderFrom = request.getHolderFrom();
        PublicKey caller = memberLookup.myInfo().getLedgerKeys().get(0);
        var holderFromKey = Objects.requireNonNull(memberLookup.lookup(holderFrom))
                .getLedgerKeys()
                .get(0);
        if(caller != holderFromKey){
            throw new IllegalArgumentException("Only owner can transfer");
        }

        // TODO normally paging and filtering would be more complex, keeping simple for poc
        var inputStateRefs =
                utxoLedgerService.findUnconsumedStatesByExactType(
                                Qrc20State.class,
                                100,
                                Instant.now())
                        .getResults()
                        .stream()
                        .filter(qrc20StateStateAndRef -> qrc20StateStateAndRef.getState()
                                .getContractState()
                                .getHolder().equals(caller))
                        .filter(qrc20StateStateAndRef -> qrc20StateStateAndRef.getState()
                                .getContractState()
                                .getSymbol().equalsIgnoreCase(request.getSymbol()))
                        //.map(StateAndRef::getState)
                        .toList();
        List<TransactionState<Qrc20State>> inputStates = inputStateRefs
                .stream()
                .map(StateAndRef::getState)
                .collect(Collectors.toList());

        var ownerSupply = inputStates.stream()
                .map(TransactionState::getContractState)
                .map(Qrc20State::getSupply)
                .reduce(Integer::sum)
                .orElse(0);

        final NotaryInfo notaryInfo = notaryLookup.getNotaryServices()
                .stream()
                .findFirst()
                .orElseThrow(RuntimeException::new);

        final MemberInfo holderTo = memberLookup.lookup(request.getHolderTo());
        if (holderTo == null) {
            throw new IllegalArgumentException(
                    String.format("The holder %s does not exist within the network", holderTo));
        }
        final PublicKey holderToKey;
        try {
            holderToKey = holderTo.getLedgerKeys().get(0);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("The holder %s has no ledger key", holderTo));
        }

        if(request.getAmount() > ownerSupply){
            throw new IllegalArgumentException("Insufficient funds");
        }

        var change = ownerSupply - request.getAmount();

        Qrc20State output1 = new Qrc20State(
                UUID.randomUUID(),
                caller,
                holderToKey,
                request.getSymbol(),
                request.getAmount(),
                List.of(caller, holderToKey)
        );
        Qrc20State output2 = new Qrc20State(
                UUID.randomUUID(),
                caller,
                caller,
                request.getSymbol(),
                change,
                List.of(caller, caller)
        );

        UtxoSignedTransaction transaction = utxoLedgerService.createTransactionBuilder()
                .setNotary(notaryInfo.getName())
                .addInputStates(inputStateRefs
                        .stream()
                        .map(StateAndRef::getRef)
                        .collect(Collectors.toList()))
                // TODO where do we set input as spent?
                .addOutputStates(output1, output2)
                .addCommand(new Qrc20Commands.Transfer())
                .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
                .addSignatories(List.of(caller, holderToKey))
                .toSignedTransaction();

        FlowSession session1 = flowMessaging.initiateFlow(holderFrom);
        FlowSession session2 = flowMessaging.initiateFlow(request.getHolderTo());

        try {
            // Send the transaction and state to the counterparty and let them sign it
            // Then notarise and record the transaction in both parties' vaults.
            utxoLedgerService.finalize(transaction, List.of(session1, session2))
                    .getTransaction().getId().toString();
            return String.format("Owner had %s, transferred %s and got change back %s",
                    ownerSupply, request.getAmount(), change);
        } catch (Exception e) {
            return String.format("Flow failed, message: %s", e.getMessage());
        }
    }
}
