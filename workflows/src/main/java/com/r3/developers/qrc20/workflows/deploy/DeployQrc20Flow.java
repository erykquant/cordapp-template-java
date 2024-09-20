package com.r3.developers.qrc20.workflows.deploy;

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
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
@InitiatingFlow(protocol = "deploy-qrc20")
public class DeployQrc20Flow implements ClientStartableFlow {

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

    public DeployQrc20Flow() {
    }

    @SuppressWarnings("DuplicatedCode")
    @Suspendable
    @Override
    @NotNull
    public String call(@NotNull final ClientRequestBody requestBody) {
        final var request = requestBody.getRequestBodyAs(jsonMarshallingService, DeployQrc20Request.class);
        final var holderName = request.getHolder();
        final var notaryInfo = notaryLookup.getNotaryServices().stream().findFirst()
                .orElseThrow(RuntimeException::new);
        final var issuerKey = memberLookup.myInfo().getLedgerKeys().get(0);
        final var holderInfo = memberLookup.lookup(holderName);
        if (holderInfo == null) {
            throw new IllegalArgumentException(String.format("The holder %s does not exist within the network", holderName));
        }
        final PublicKey holderKey;
        try {
            holderKey = holderInfo.getLedgerKeys().get(0);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("The holder %s has no ledger key", holderName));
        }

        final var qrc20 = new Qrc20State(
                UUID.randomUUID(),
                issuerKey,
                holderKey,
                request.getSymbol(),
                request.getSupply(),
                List.of(issuerKey, holderKey)
        );
        final var transaction = utxoLedgerService.createTransactionBuilder()
                .setNotary(notaryInfo.getName())
                .addOutputState(qrc20)
                .addCommand(new Qrc20Commands.Deploy())
                .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
                .addSignatories(List.of(issuerKey, holderKey))
                .toSignedTransaction();

        final var session = flowMessaging.initiateFlow(holderName);

        try {
            utxoLedgerService.finalize(transaction, List.of(session));
            return qrc20.toString();
        } catch (Exception e) {
            return String.format("Flow failed, message: %s", e.getMessage());
        }
    }
}
