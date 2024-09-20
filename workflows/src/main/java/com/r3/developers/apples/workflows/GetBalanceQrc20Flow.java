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

@InitiatingFlow(protocol = "getbalance-qrc20")
public class GetBalanceQrc20Flow implements ClientStartableFlow {

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

    public GetBalanceQrc20Flow() {
    }

    @Suspendable
    @Override
    @NotNull
    public String call(@NotNull ClientRequestBody requestBody) {
        GetBalanceQrc20Request request = requestBody.getRequestBodyAs(
                jsonMarshallingService, GetBalanceQrc20Request.class);
        var holder = request.getHolder();
        var holderKey = Objects.requireNonNull(memberLookup.lookup(holder))
                .getLedgerKeys()
                .get(0);

        // TODO normally paging and filtering would be more complex, keeping simple for poc
        List<TransactionState<Qrc20State>> allTokenStateAndRefs =
                utxoLedgerService.findUnconsumedStatesByExactType(
                                Qrc20State.class,
                                100,
                                Instant.now())
                        .getResults()
                        .stream()
                        .filter(qrc20StateStateAndRef -> qrc20StateStateAndRef.getState()
                                .getContractState()
                                .getHolder().equals(holderKey))
                        .filter(qrc20StateStateAndRef -> qrc20StateStateAndRef.getState()
                                .getContractState()
                                .getSymbol().equalsIgnoreCase(request.getSymbol()))
                        .map(StateAndRef::getState).toList();

        var ownerSupply = allTokenStateAndRefs.stream()
                .map(TransactionState::getContractState)
                .map(Qrc20State::getSupply)
                .reduce(Integer::sum)
                .orElse(0);

        final NotaryInfo notaryInfo = notaryLookup.getNotaryServices()
                .stream()
                .findFirst()
                .orElseThrow(RuntimeException::new);


        Qrc20State output1 = new Qrc20State(
                UUID.randomUUID(),
                holderKey,
                holderKey,
                request.getSymbol(),
                ownerSupply,
                List.of(holderKey, holderKey)
        );


        FlowSession session1 = flowMessaging.initiateFlow(holder);
        return ownerSupply.toString();
    }
}
