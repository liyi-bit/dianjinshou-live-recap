package com.dianjinshou.modules.ticket.service;

import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.ticket.dto.CreateTicketRequest;
import com.dianjinshou.modules.ticket.entity.CustomerServiceTicket;
import com.dianjinshou.modules.ticket.mapper.TicketMapper;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private final TicketMapper ticketMapper;

    public TicketService(TicketMapper ticketMapper) {
        this.ticketMapper = ticketMapper;
    }

    public CustomerServiceTicket createTicket(CreateTicketRequest request) {
        Long userId = SecurityContextHelper.currentUserId();

        CustomerServiceTicket ticket = new CustomerServiceTicket();
        ticket.setUserId(userId);
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setContact(request.getContact());
        ticket.setStatus("OPEN");
        ticketMapper.insert(ticket);
        return ticket;
    }
}
