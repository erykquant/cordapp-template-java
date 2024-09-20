package com.r3.developers.qrc20.workflows.balance;

import net.corda.v5.base.types.MemberX500Name;

@SuppressWarnings("unused")
public class BalanceQrc20Request {

    private MemberX500Name holder;
    private String symbol;

    public BalanceQrc20Request() {
    }

    public BalanceQrc20Request(final MemberX500Name holder, final String symbol) {
        this.holder = holder;
        this.symbol = symbol;
    }


    public String getSymbol() {
        return symbol;
    }


    public MemberX500Name getHolder() {
        return holder;
    }
}
