package com.r3.developers.qrc20.workflows.mint;

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
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"DuplicatedCode", "unused"})
@InitiatingFlow(protocol = "mint-qrc20")
public class MintQrc20Flow implements ClientStartableFlow {

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

    public MintQrc20Flow() {
    }

    @Suspendable
    @Override
    @NotNull
    public String call(@NotNull final ClientRequestBody requestBody) {
        final var request = requestBody.getRequestBodyAs(jsonMarshallingService, MintQrc20Request.class);
        final var callerKey = memberLookup.myInfo().getLedgerKeys().get(0);

        // normally paging and filtering would be more complex, keeping it simple for POC
        final var inputStateAndRefs = utxoLedgerService.findUnconsumedStatesByExactType(
                        Qrc20State.class, 10000, Instant.now())
                .getResults()
                .stream()
                .filter(qrc20StateStateAndRef -> qrc20StateStateAndRef.getState()
                        .getContractState()
                        .getSymbol()
                        .equals(request.getSymbol()))
                .toList();
        final var transactionStates = inputStateAndRefs
                .stream()
                .map(StateAndRef::getState)
                .toList();
        final NotaryInfo notaryInfo = notaryLookup.getNotaryServices()
                .stream()
                .findFirst()
                .orElseThrow(RuntimeException::new);
        final var issuerKey = transactionStates.get(0).getContractState().getIssuer();

        if (issuerKey != callerKey) {
            throw new IllegalArgumentException("Only issuer can mint");
        }

        final var output = new Qrc20State(
                UUID.randomUUID(),
                issuerKey,
                issuerKey,
                request.getSymbol(),
                request.getAmount(),
                List.of(issuerKey, issuerKey));

        final var transaction = utxoLedgerService.createTransactionBuilder()
                .setNotary(notaryInfo.getName())
                .addOutputStates(output)
                .addCommand(new Qrc20Commands.Mint())
                .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
                .addSignatories(List.of(callerKey, callerKey))
                .toSignedTransaction();

        final var session = flowMessaging.initiateFlow(Objects.requireNonNull(
                        memberLookup.lookup(issuerKey))
                .getName());

        try {
            utxoLedgerService.finalize(transaction, List.of(session))
                    .getTransaction().getId();
            return String.format("Minted %s %s", request.getAmount(), request.getSymbol());
        } catch (Exception e) {
            return String.format("Flow failed, message: %s", e.getMessage());
        }
    }
}
