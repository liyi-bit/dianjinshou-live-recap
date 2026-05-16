package com.dianjinshou.modules.ticket.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.ticket.dto.CreateTicketRequest;
import com.dianjinshou.modules.ticket.entity.CustomerServiceTicket;
import com.dianjinshou.modules.ticket.service.TicketService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/settings/customer-service-tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ApiResponse<CustomerServiceTicket> create(@Valid @RequestBody CreateTicketRequest request) {
        return ApiResponse.success(ticketService.createTicket(request));
    }
}
