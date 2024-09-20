package com.r3.developers.qrc20.workflows.burn;

import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatedBy;
import net.corda.v5.application.flows.ResponderFlow;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.ledger.utxo.FinalizationResult;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@InitiatedBy(protocol = "burn-qrc20")
public class BurnQrc20ResponderFlow implements ResponderFlow {

    @CordaInject
    public UtxoLedgerService utxoLedgerService;

    @Override
    @Suspendable
    public void call(@NotNull FlowSession session) {
        FinalizationResult finalizationResult = utxoLedgerService.receiveFinality(session, _transaction -> {
        });
    }
}
