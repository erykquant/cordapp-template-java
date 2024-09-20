package com.r3.developers.qrc20.workflows.burn;

import net.corda.v5.base.types.MemberX500Name;

@SuppressWarnings("unused")
public class BurnQrc20Request {

    private MemberX500Name holder;
    private String symbol;
    private int amount;

    public BurnQrc20Request() {
    }

    public BurnQrc20Request(final MemberX500Name holder,
                            final String symbol,
                            final int amount) {
        this.holder = holder;
        this.symbol = symbol;
        this.amount = amount;
    }

    public MemberX500Name getHolder() {
        return holder;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getAmount() {
        return amount;
    }
}
