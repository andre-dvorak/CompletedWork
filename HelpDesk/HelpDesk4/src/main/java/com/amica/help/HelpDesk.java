package com.amica.help;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amica.help.Ticket.Priority;
import com.amica.help.Ticket.Status;

/**
 * Implementation of the primary API. The help desk collects technicians
 * and tickets, cooreindates creation and lifecycle of tickets, and offers
 * query methods to get one or more tickets by a few useful criteria.
 * It also holds a tag manager, so that keyword tags assigned to tickets
 * are unique objects within the scope of this help desk instance.
 * 
 * @author Will Provost
 */
public class HelpDesk implements HelpDeskAPI {

	private int nextID = 0;
	private SortedSet<Technician> technicians = new TreeSet<>();
	private SortedSet<Ticket> tickets = new TreeSet<>();
	private Tags tags;

	public HelpDesk() {
		this(new Tags());
	}
	
	public HelpDesk(Tags tags) {
		this.tags = tags;
	}

	public Tags getTags() {
		return tags;
	}

	public void addTechnician(String ID, String name, int extension) {
		technicians.add(new Technician(ID, name, extension));
	}

	public int createTicket(String originator, 
				String description, Priority priority) {
		if (technicians.isEmpty()) {
			throw new IllegalStateException("No technicians available yet.");
		}
		Ticket ticket = new Ticket(++nextID, originator, description, priority);
		tickets.add(ticket);
		ticket.assign(technicians.stream()
			.min(Comparator.comparing(t -> t.getActiveTickets().count()))
			.get());
		return ticket.getID();
	}

	public void addTags(int ID, String... tagValues) {
		Ticket ticket = getTicketByID(ID);
		if (ticket != null) {
			for (String tagValue : tagValues) {
				ticket.addTag(tags.getTag(tagValue));
			}
		} else {
			throw new IllegalArgumentException("No ticket with ID " + ID);
		}
	}

	public SortedSet<Technician> getTechnicians() {
		return technicians;
	}

	public int reopenTicket(int priorTicketID, String reason, Priority priority) {
		if (technicians.isEmpty()) {
			throw new IllegalStateException("No technicians available yet.");
		}
		Ticket ticket = new ReopenedTicket
				(++nextID, getTicketByID(priorTicketID), reason, priority);
		tickets.add(ticket);
		return ticket.getID();
	}

	public Stream<Ticket> getTickets() {
		return tickets.stream();
	}

	public Ticket getTicketByID(int ID) {
		return tickets.stream()
				.filter(t -> t.getID() == ID).findFirst().orElse(null);
	}

	public Stream<Ticket> getTicketsByStatus(Status status) {
		return tickets.stream().filter(t -> t.getStatus() == status);
	}

	public Stream<Ticket> getTicketsByNotStatus(Status status) {
		return tickets.stream().filter(t -> t.getStatus() != status);
	}

	public Stream<Ticket> getTicketsByTechnician(String techID) {
		return tickets.stream()
				.filter(t -> t.getTechnician().getID().equals(techID));
	}

	public Stream<Ticket> getTicketsWithAnyTag(String... tagValues) {
		return tickets.stream().filter(ticket -> ticket.getTags().anyMatch
			(candidate -> Arrays.stream(tagValues).map(tags::getTag)
				.anyMatch(tag -> tag.equals(candidate))));
	}

	public int getAverageMinutesToResolve() {
		return (int) getTicketsByStatus(Status.RESOLVED)
				.mapToInt(Ticket::getMinutesToResolve).average()
				.getAsDouble();
	}

	public Map<String, Double> getAverageMinutesToResolvePerTechnician() {
		return getTicketsByStatus(Status.RESOLVED)
			.collect(Collectors.groupingBy(t -> t.getTechnician().getID(),
				Collectors.averagingInt(Ticket::getMinutesToResolve)));
	}

	public Stream<Ticket> getTicketsByText(String text) {
		return tickets.stream().filter(t -> t.includesText(text));
	}
	
	public Stream<Event> getLatestActivity(int count) {
		return tickets.stream()
				.flatMap(Ticket::getHistory)
				.sorted(Collections.reverseOrder())
				.limit(count);
	}
}
