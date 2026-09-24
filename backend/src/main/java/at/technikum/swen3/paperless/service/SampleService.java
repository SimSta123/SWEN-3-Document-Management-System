package at.technikum.swen3.paperless.service;

import at.technikum.swen3.paperless.repository.SampleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SampleService {

    private final SampleRepository repository;

    public SampleService(SampleRepository repository) {
        this.repository = repository;
    }

    public String dbcheck() {
        return repository.getCheck();
    }
}
