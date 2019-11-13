package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.sun.javafx.geom.transform.Identity
import com.template.contracts.IOUContract
import com.template.states.IOUState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import java.util.*
import net.corda.core.identity.AbstractParty

// *********
// * Flows *
// *********

@InitiatingFlow
@StartableByRPC
class IOUIssueFlow(val amount : Amount<Currency>,
                   val BParty : Party) : FlowLogic<SignedTransaction>(){


    override val progressTracker = ProgressTracker()

    @Suspendable
    override  fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val opState = IOUState(amount,ourIdentity,BParty)
        val issuedCommand = Command(IOUContract.Commands.Issue(),listOf(ourIdentity.owningKey , BParty.owningKey))

        val txBuilder = TransactionBuilder(notary = notary)
                .addOutputState(opState,IOUContract.id)
                .addCommand(issuedCommand)

        txBuilder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(txBuilder)


        val flowsession = initiateFlow(BParty)
        val allSignedTransaction = subFlow(CollectSignaturesFlow(signedTx , listOf(flowsession),CollectSignaturesFlow.tracker()))
        subFlow(FinalityFlow(allSignedTransaction,listOf(flowsession)))


        return allSignedTransaction
    }
}

@InitiatingFlow
@StartableByRPC
class IOUTransferFlow(val stateRef : StateRef,
                      val amount : Amount<Currency>,
                      val Receiver : AbstractParty ) : FlowLogic<SignedTransaction>(){

    override val progressTracker = ProgressTracker()
    @Throws(InsufficientBalanceException::class)

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val InState = serviceHub.toStateAndRef<IOUState>(stateRef)
        // val input = tx.inputsOfType<IOUState>().single()

        val OpState = InState.state.data.withtrx(amount,Receiver).ownableState
        val OpState2 = InState.state.data.withTransfer(amount).ownableState
        // val OpState3 = InState.state.data.withNewOwner(Receiver).ownableState

        //IOUState(amount,ourIdentity,Receiver)

        //val state = IOUState(amount,ourIdentity,Receiver)
        val TransferCommand = Command(IOUContract.Commands.Transfer(), listOf(ourIdentity.owningKey , Receiver.owningKey))

        val txBuilder = TransactionBuilder( notary = notary)
                .addInputState(InState)
                .addOutputState(OpState)
                .addOutputState(OpState2)
                .addCommand(TransferCommand)

//        CashUtils.generateSpend(
//                services = serviceHub,
//                tx = txBuilder,
//                amount = amount,
//                ourIdentity = stateRef.
//
//        )
        txBuilder.verify(serviceHub)

        val signedTx = serviceHub.signInitialTransaction(txBuilder)
        val transferSession = initiateFlow(Receiver as Party)
        //(state.participants - ourIdentity).map { initiateFlow(it as Party) }.toSet()
        val fullTx = subFlow( CollectSignaturesFlow(signedTx , listOf(transferSession) , CollectSignaturesFlow.tracker() ))
        subFlow(FinalityFlow( fullTx , transferSession ) )

        return fullTx

    }
}



@InitiatedBy(IOUIssueFlow::class)
class IOUIssueFlowResponder( private val flowsession : FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowsession){
            override fun checkTransaction(stx: SignedTransaction) =
                    requireThat {
                        val output = stx.tx.outputs.single().data
                        "This must be an IOU transaction " using (output is IOUState)
                    }

        }
        val expectedTxId = subFlow(signedTransactionFlow).id
        subFlow(ReceiveFinalityFlow(flowsession , expectedTxId))

    }
}


@InitiatedBy(IOUTransferFlow :: class)
class IOUTransferResponder( private val transferSession: FlowSession) : FlowLogic <Unit> (){

    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(transferSession){
            override fun checkTransaction(stx: SignedTransaction) =
                    requireThat {
                        val output1 = stx.tx.outputs.get(1).data
                        " This must be an IOU state transaction " using (output1 is IOUState)
                    }

        }
        val expectedTxId = subFlow(signedTransactionFlow).id
        subFlow(ReceiveFinalityFlow(transferSession , expectedTxId))
        //subFlow(signedTransactionFlow)
    }

}


