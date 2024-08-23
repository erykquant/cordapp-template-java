package com.r3.developers.apples.workflows;

import net.corda.v5.base.types.MemberX500Name;

public class CreateAndIssueQrc20Request {

    private MemberX500Name holder;
    private MemberX500Name notary;
    private String symbol;
    private int supply;

    // The JSON Marshalling Service, which handles serialisation, needs this constructor.
    public CreateAndIssueQrc20Request() {
    }

    public CreateAndIssueQrc20Request(MemberX500Name holder, MemberX500Name notary,
                                      String symbol, int supply) {
        this.holder = holder;
        this.notary = notary;
        this.symbol = symbol;
        this.supply = supply;
    }

    public MemberX500Name getHolder() {
        return holder;
    }

    public MemberX500Name getNotary() {
        return notary;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getSupply() {
        return supply;
    }
}
