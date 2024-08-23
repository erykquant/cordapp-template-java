package com.r3.developers.apples.contracts;

import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class Qrc20Contract implements Contract {
    @Override
    public void verify(@NotNull final UtxoLedgerTransaction transaction) {
        if (transaction.getCommands().size() != 1) {
            throw new IllegalArgumentException("No single command provided");
        }
        final Command command = transaction.getCommands().get(0);
    }
}
