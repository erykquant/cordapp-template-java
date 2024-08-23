package com.r3.developers.apples.workflows;

import com.r3.developers.apples.contracts.AppleCommands;
import com.r3.developers.apples.contracts.Qrc20Commands;
import com.r3.developers.apples.contracts.Qrc20Contract;
import com.r3.developers.apples.states.AppleStamp;
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
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@InitiatingFlow(protocol = "create-and-issue-qrc20")
public class CreateAndIssueQrc20Flow implements ClientStartableFlow {

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

    public CreateAndIssueQrc20Flow() {}

    @Suspendable
    @Override
    @NotNull
    public String call(@NotNull ClientRequestBody requestBody) {

        CreateAndIssueQrc20Request request = requestBody.getRequestBodyAs(jsonMarshallingService, CreateAndIssueQrc20Request.class);
        MemberX500Name holderName = request.getHolder();

        final NotaryInfo notaryInfo = notaryLookup.lookup(request.getNotary());
        if (notaryInfo == null) {
            throw new IllegalArgumentException("Notary " + request.getNotary() + " not found");
        }

        PublicKey issuer = memberLookup.myInfo().getLedgerKeys().get(0);

        final MemberInfo holderInfo = memberLookup.lookup(holderName);
        if (holderInfo == null) {
            throw new IllegalArgumentException(String.format("The holder %s does not exist within the network", holderName));
        }

        final PublicKey holder;
        try {
            holder = holderInfo.getLedgerKeys().get(0);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("The holder %s has no ledger key", holderName));
        }

        Qrc20State newQrc20 = new Qrc20State(
                UUID.randomUUID(),
                issuer,
                holder,
                request.getSymbol(),
                request.getSupply(),
                List.of(issuer, holder)
        );

        UtxoSignedTransaction transaction = utxoLedgerService.createTransactionBuilder()
                .setNotary(notaryInfo.getName())
                .addOutputState(newQrc20)
                .addCommand(new Qrc20Commands.Issue())
                .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
                .addSignatories(List.of(issuer, holder))
                .toSignedTransaction();

        FlowSession session = flowMessaging.initiateFlow(holderName);

        try {
            // Send the transaction and state to the counterparty and let them sign it
            // Then notarise and record the transaction in both parties' vaults.
            utxoLedgerService.finalize(transaction, List.of(session));
            return newQrc20.getId().toString();
        } catch (Exception e) {
            return String.format("Flow failed, message: %s", e.getMessage());
        }
    }
}
