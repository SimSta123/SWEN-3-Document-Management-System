package at.technikum.swen3.paperless.controller;

import at.technikum.swen3.paperless.service.SampleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/one")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class SampleController {

    private final SampleService service;

    public SampleController(SampleService service) {
        this.service = service;
    }

    @GetMapping("")
    public ResponseEntity<String> dbcheck() {
        return ResponseEntity.ok(service.dbcheck());
    }

    @GetMapping("/i")
    public ResponseEntity<String> versuch() {
        return ResponseEntity.ok("working");
    }

}