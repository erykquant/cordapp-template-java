package com.r3.developers.qrc20.workflows.transfer;

import net.corda.v5.base.types.MemberX500Name;

@SuppressWarnings("unused")
public class TransferQrc20Request {

    private MemberX500Name holderFrom;
    private MemberX500Name holderTo;
    private String symbol;
    private int amount;

    public TransferQrc20Request() {
    }

    public TransferQrc20Request(final MemberX500Name holderFrom,
                                final MemberX500Name holderTo,
                                final String symbol,
                                final int amount) {
        this.holderFrom = holderFrom;
        this.holderTo = holderTo;
        this.symbol = symbol;
        this.amount = amount;
    }

    public MemberX500Name getHolderFrom() {
        return holderFrom;
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
