package com.cibertec.app.controller;

import com.cibertec.app.entity.Pack;
import com.cibertec.app.service.PackService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/packs")
public class PackController {

    @Autowired private PackService packService;

    @GetMapping
    public ResponseEntity<List<Pack>> listarPacks() {
        List<Pack> lista = packService.listarTodos();
        return ResponseEntity.ok(lista);
    }
}
