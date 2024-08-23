package com.r3.developers.apples.states;

import com.r3.developers.apples.contracts.Qrc20Contract;
import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;

import java.security.PublicKey;
import java.util.List;
import java.util.UUID;

@BelongsToContract(Qrc20Contract.class)
public class Qrc20State implements ContractState {
    private UUID id;
    private PublicKey issuer;
    private PublicKey holder;
    private String symbol;
    private int supply;
    private List<PublicKey> participants;

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
}
