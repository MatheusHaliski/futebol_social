package com.footballai.social.controller;
import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.service.DiscoveryService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.*;
@RestController @RequestMapping("/v1/discovery")
public class DiscoveryController {
 private final DiscoveryService service;public DiscoveryController(DiscoveryService service){this.service=service;}
 @GetMapping("/fields") public List<DiscoveryService.Field> fields(){return service.fields();}
 @GetMapping public DiscoveryService.Results search(@AuthenticationPrincipal AuthPrincipal p,
   @RequestParam(defaultValue="ALL") String entity,@RequestParam(required=false) String q,
   @RequestParam(name="filter",required=false) List<String> filters,@RequestParam(defaultValue="0") int page,
   @RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="name") String order){
  return service.search(p,entity,q,filters==null?List.of():filters,page,size,order);
 }
}
