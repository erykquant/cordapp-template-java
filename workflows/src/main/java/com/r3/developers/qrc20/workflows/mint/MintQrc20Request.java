package com.r3.developers.qrc20.workflows.mint;

@SuppressWarnings("unused")
public class MintQrc20Request {

    private String symbol;
    private int amount;

    public MintQrc20Request() {
    }

    public MintQrc20Request(final String symbol,
                            final int amount) {
        this.symbol = symbol;
        this.amount = amount;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getAmount() {
        return amount;
    }
}
