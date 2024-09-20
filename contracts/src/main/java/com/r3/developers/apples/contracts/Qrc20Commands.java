package com.r3.developers.apples.contracts;

import net.corda.v5.ledger.utxo.Command;

public interface Qrc20Commands extends Command {

    class Issue implements Qrc20Commands {

    }

    class Transfer implements Qrc20Commands {
    }

    class Mint implements Qrc20Commands {
    }

    class Burn implements Qrc20Commands {
    }

    class Balance implements Qrc20Commands{

    }
}
