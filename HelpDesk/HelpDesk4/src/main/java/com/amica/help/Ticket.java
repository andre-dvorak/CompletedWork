package com.amica.help;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Class representing a problem ticket for a help desk.
 *
 * @author Will Provost
 */
@Getter
@EqualsAndHashCode(of="ID")
public class Ticket implements Comparable<Ticket> {

	public enum Status { CREATED, ASSIGNED, WAITING, RESOLVED }
	public enum Priority { LOW, MEDIUM, HIGH, URGENT }

	private int ID;
	private Priority priority;
	private String originator;
	private String description;
	private Technician technician;
	private List<Event> history = new ArrayList<>();
	private SortedSet<Tag> tags = new TreeSet<>();

	public Ticket(int ID, String originator, String description, Priority priority) {
		if(originator == null || description == null || priority == null){
			throw new IllegalArgumentException();
		}

		this.ID = ID;
		this.priority = priority;
		this.originator = originator;
		this.description = description;
		this.history.add(new Event(this, Status.CREATED, "Created ticket."));
	}

	public Status getStatus() {
		return history.stream()
				.map(Event::getNewStatus)
				.filter(Objects::nonNull)
				.reduce((a,b) -> b)
				.get();
	}

	public Stream<Event> getHistory() {
		return history.stream();
	}

	public Stream<Tag> getTags() {
		return tags.stream();
	}
    
	public void assign(Technician technician) {
		if(technician == null){
			throw new IllegalArgumentException("Technician cannot be null");
		}

		if (getStatus() == Status.CREATED || getStatus() == Status.ASSIGNED) {
			if (this.technician != null) {
				this.technician.resolveTicket(this);
			}
			
			this.technician = technician;
			Status newStatus = Status.ASSIGNED;
			history.add(new Event(this, newStatus, "Assigned to " + technician + "."));
			technician.assignTicket(this);
		} else if (getStatus() == Status.RESOLVED) {
			throw new IllegalStateException("Can't re-assign a resolved new ticket.");
		} else {
			throw new IllegalStateException("Can't resolve a ticket with status " + getStatus());
		}
	}

    public void suspend(String reason) {
		if(reason == null){
			throw new IllegalArgumentException("Reason cannot be null");
		}

		if (getStatus() == Status.ASSIGNED) {
			history.add(new Event(this, Status.WAITING, reason));
		} else {
			throw new IllegalStateException("Can't suspend until the ticket is assigned.");
		}
    }
    
    public void resume(String reason) {
		if(reason == null){
			throw new IllegalArgumentException("Reason cannot be null");
		}

		if (getStatus() == Status.WAITING) {
			history.add(new Event(this, Status.ASSIGNED, reason));
		} else {
			throw new IllegalStateException("Can't resume a ticket that isn't in the WAITING state.");
		}
    }
	public void addNote(String note) {
		if(note == null){
			throw new IllegalArgumentException("Note cannot be null");
		}

		history.add(new Event(this, note));
	}

	public void resolve(String reason) {
		if(reason == null){
			throw new IllegalArgumentException("Reason cannot be null");
		}

		if (getStatus() == Status.ASSIGNED) {
			history.add(new Event(this, Status.RESOLVED, reason));
			technician.resolveTicket(this);
		} else if (getStatus() == Status.CREATED) {
			throw new IllegalStateException("Can't resolve until the ticket is assigned.");
		} else if (getStatus() == Status.RESOLVED) {
			throw new IllegalStateException("Can't resolve a resolved ticket.");
		} else {
			throw new IllegalStateException("Can't resolve a ticket with status " + getStatus());
		}
	}

	public boolean addTag(Tag tag) {
		return tags.add(tag);
	}

	public int getMinutesToResolve() {
		final int MILLISECONDS_PER_MINUTE = 60000;
		if (getStatus() == Status.RESOLVED) {
			long time = history.get(history.size() - 1).getTimestamp() - history.get(0).getTimestamp();
			return (int) time / MILLISECONDS_PER_MINUTE;
		} else {
			throw new IllegalStateException("The ticket is not yet resolved.");
		}
	}
    
	public boolean includesText(String text) {
		return description.contains(text) 
				|| getHistory().map(Event::getNote).anyMatch(n -> n.contains(text));
    }
    
	@Override
	public String toString() {
		return String.format("Ticket %d: %s priority, %s", 
				ID, priority.toString(), getStatus().toString());
	}
    
	public int compareTo(Ticket other) {
		if (this.equals(other)) {
			return 0;
		}

		int result = -priority.compareTo(other.getPriority());
		if (result == 0) {
			result = Integer.compare(ID, other.getID());
		}
		return result;
	}
}
