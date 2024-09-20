package com.r3.developers.qrc20.states;

import com.r3.developers.qrc20.contracts.Qrc20Contract;
import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.base.annotations.CordaSerializable;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;
import java.util.UUID;


@SuppressWarnings("ClassCanBeRecord")
@BelongsToContract(Qrc20Contract.class)
@CordaSerializable
public class Qrc20State implements ContractState {
    private final UUID id;
    private final PublicKey issuer;
    private final PublicKey holder;
    private final String symbol;
    private final int supply;
    private final List<PublicKey> participants;

    @ConstructorForDeserialization
    public Qrc20State(final UUID id,
                      final PublicKey issuer,
                      final PublicKey holder,
                      final String symbol,
                      final int supply,
                      final List<PublicKey> participants) {
        this.id = id;
        this.issuer = issuer;
        this.holder = holder;
        this.symbol = symbol;
        this.supply = supply;
        this.participants = participants;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return participants;
    }

    public UUID getId() {
        return this.id;
    }

    public PublicKey getIssuer() {
        return this.issuer;
    }

    public PublicKey getHolder() {
        return this.holder;
    }

    public String getSymbol() {
        return this.symbol;
    }

    public int getSupply() {
        return this.supply;
    }

    @Override
    public String toString() {
        return "Qrc20State{"
                + "id=" + id
                + ",symbol='" + symbol
                + ",supply=" + supply
                + "}";
    }
}
