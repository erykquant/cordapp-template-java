package com.r3.developers.qrc20.workflows.deploy;

import net.corda.v5.base.types.MemberX500Name;

@SuppressWarnings("unused")
public class DeployQrc20Request {

    private MemberX500Name holder;
    private String symbol;
    private int supply;

    public DeployQrc20Request() {
    }

    public DeployQrc20Request(final MemberX500Name holder,
                              final String symbol,
                              final int supply) {
        this.holder = holder;
        this.symbol = symbol;
        this.supply = supply;
    }

    public MemberX500Name getHolder() {
        return holder;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getSupply() {
        return supply;
    }
}
