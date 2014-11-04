package com.rizzo.trifle.web;

import akka.actor.ActorRef;
import com.rizzo.trifle.domain.CrawlProcess;
import com.rizzo.trifle.domain.CrawlResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TrifleRestController {

    private ActorRef supervisor;

    public TrifleRestController() {
    }

    public TrifleRestController(ActorRef supervisor) {
        this.supervisor = supervisor;
    }

    @RequestMapping(value = "/seed", method = RequestMethod.PUT)
    public @ResponseBody ResponseEntity<CrawlResponse> seed(@RequestBody CrawlProcess crawlProcess){
        final CrawlProcess process = crawlProcess.checkIds();
        this.supervisor.tell(process, null);
        return new ResponseEntity<>(new CrawlResponse()
                .setResponse("Seed swallowed!")
                .setProcessId(process.getId()), HttpStatus.OK);
    }

}
