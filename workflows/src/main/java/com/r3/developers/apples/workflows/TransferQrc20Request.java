package com.r3.developers.apples.workflows;

import net.corda.v5.base.types.MemberX500Name;

public class TransferQrc20Request {

    private MemberX500Name holderFrom;
    private MemberX500Name holderTo;
    private MemberX500Name notary;
    private String symbol;
    private int amount;

    // The JSON Marshalling Service, which handles serialisation, needs this constructor.
    public TransferQrc20Request() {
    }

    public TransferQrc20Request(
            MemberX500Name holderFrom,
            MemberX500Name holderTo,
            MemberX500Name notary,
            String symbol,
            int amount) {
        this.holderFrom = holderFrom;
        this.holderTo = holderTo;
        this.notary = notary;
        this.symbol = symbol;
        this.amount = amount;
    }

    public MemberX500Name getHolderFrom() {
        return holderFrom;
    }

    public MemberX500Name getNotary() {
        return notary;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getAmount() {
        return amount;
    }

    public MemberX500Name getHolderTo() {
        return holderTo;
    }
}
