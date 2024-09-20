package com.r3.developers.apples.workflows;

import net.corda.v5.base.types.MemberX500Name;

public class GetBalanceQrc20Request {

    private MemberX500Name holder;
    private String symbol;

    // The JSON Marshalling Service, which handles serialisation, needs this constructor.
    public GetBalanceQrc20Request() {
    }

    public GetBalanceQrc20Request(
            MemberX500Name holder,
            String symbol) {
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
