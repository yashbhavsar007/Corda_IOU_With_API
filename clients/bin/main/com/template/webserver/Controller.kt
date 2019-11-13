package com.template.webserver

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import net.corda.core.identity.CordaX500Name
import net.corda.core.contracts.Amount
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import javax.servlet.http.HttpServletRequest
import net.corda.core.messaging.startTrackedFlow
import com.template.flows.IOUIssueFlow
import net.corda.core.utilities.getOrThrow

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
    private fun templateendpoint(): String {
        return "Define an endpoint here."
    }

    @GetMapping(value = "/test", produces = arrayOf("text/plain "))
    private  fun test():String{
        return "TESTING HERE"
    }

    @PostMapping(value = "/issue", produces = ["application/json"],  headers = ["Content-Type=application/json"])
    private fun CallingFlow(request:HttpServletRequest):ResponseEntity<String>{
        val amount = request.getParameter("amount")
        val am = Amount.parseCurrency(amount)
        val pname = request.getParameter("partyName")

        if(pname == null){
            return ResponseEntity.badRequest().body("Query parameter 'partyName' must not be null.\n")
        }
        if(am <= Amount.parseCurrency("0") ){
            return ResponseEntity.badRequest().body("Query parameter 'amount' must be greater than 0.\n")
        }

        val partyX500Name = CordaX500Name.parse(pname)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $pname cannot be found.\n")

        return try{
            val signedTx = proxy.startTrackedFlow(::IOUIssueFlow, am,otherParty).returnValue.getOrThrow()

            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex:Throwable) {

            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }
}