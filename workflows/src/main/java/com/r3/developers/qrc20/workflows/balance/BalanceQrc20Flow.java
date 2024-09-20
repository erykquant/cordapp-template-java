package com.r3.developers.qrc20.workflows.balance;

import com.r3.developers.qrc20.states.Qrc20State;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatingFlow;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.TransactionState;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Objects;

@SuppressWarnings("unused")
@InitiatingFlow(protocol = "balance-qrc20")
public class BalanceQrc20Flow implements ClientStartableFlow {

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    @CordaInject
    public MemberLookup memberLookup;

    @CordaInject
    UtxoLedgerService utxoLedgerService;

    public BalanceQrc20Flow() {
    }

    @Suspendable
    @Override
    @NotNull
    public String call(@NotNull final ClientRequestBody requestBody) {
        var request = requestBody.getRequestBodyAs(jsonMarshallingService, BalanceQrc20Request.class);
        var holderName = request.getHolder();
        var holderKey = Objects.requireNonNull(memberLookup.lookup(holderName))
                .getLedgerKeys()
                .get(0);

        // normally paging and filtering would be more complex, keeping it simple for POC
        var transactionStates = utxoLedgerService.findUnconsumedStatesByExactType(
                        Qrc20State.class, 10000, Instant.now())
                .getResults()
                .stream()
                .filter(qrc20StateStateAndRef -> qrc20StateStateAndRef.getState()
                        .getContractState()
                        .getHolder()
                        .equals(holderKey))
                .filter(qrc20StateStateAndRef -> qrc20StateStateAndRef.getState()
                        .getContractState()
                        .getSymbol()
                        .equals(request.getSymbol()))
                .map(StateAndRef::getState).toList();

        return String.valueOf(transactionStates.stream()
                .map(TransactionState::getContractState)
                .map(Qrc20State::getSupply)
                .reduce(Integer::sum)
                .orElse(0));
    }
}
