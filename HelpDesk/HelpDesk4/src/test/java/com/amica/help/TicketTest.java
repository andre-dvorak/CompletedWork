package com.amica.help;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.amica.help.Ticket.Priority;
import com.amica.help.Ticket.Status;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.stream.Collectors;

/**
 * Unit test for the {@link Ticket} class.
 * 
 * @author Will Provost
 */
public class TicketTest {

  public static final String TECHNICIAN1_ID = "TECHNICIAN1_ID";
  public static final String TECHNICIAN1_NAME = "TECHNICIAN1_NAME";
  public static final int TECHNICIAN1_EXT = 12345;

  public static final String TECHNICIAN2_ID = "TECHNICIAN2_ID";
  public static final String TECHNICIAN2_NAME = "TECHNICIAN2_NAME";
  public static final int TECHNICIAN2_EXT = 56789;

  public static final int ID = 1;
  public static final String ORIGINATOR = "ORIGINATOR";
  public static final String DESCRIPTION = "DESCRIPTION";
  public static final Priority PRIORITY = Priority.HIGH;
  public static final String RESOLVE_REASON = "RESOLVE_REASON";
  public static final String WAIT_REASON = "WAIT_REASON";
  public static final String RESUME_REASON = "RESUME_REASON";
  public static final String NOTE = "NOTE";
  public static final Tag TAG1 = new Tag("TAG1");
  public static final Tag TAG2 = new Tag("TAG2");
      
  public static final String START_TIME = "1/3/22 13:37";
  
  protected Ticket ticket;
  protected Technician technician1;
  
  /**
   * Custom matcher that assures that an {@link Event} added to a ticket
   * has the expected ticket ID, timestamp, status, and note. 
   */
  protected Matcher<Event> eventWith(Status status, String note) {
    return allOf(instanceOf(Event.class),
        hasProperty("ticketID", equalTo(ID)),
        hasProperty("timestamp", equalTo(Clock.getTime())),
        hasProperty("newStatus", equalTo(status)),
        hasProperty("note", equalTo(note)));
  }
  
  /**
   * Helper method to assert that the Nth (0-based) event on the target ticket
   * has the expected ID, timestamp, status, and note.
   */
  protected void assertHasEvent(int index, Status status, String note) {
    assertThat(ticket.getHistory().count(), equalTo(index + 1L));
    assertThat(ticket.getHistory().skip(index).findFirst().get(),
        eventWith(status, note));
  }

  /**
   * Helper method to set clock forward one minute
   */
  protected void passOneMinute(){
    Clock.setTime(Clock.getTime() + 60000);
  }

  /**
   * Moves clock forward one minute and assigns ticket to technician1
   */
  protected void assignTicket(){
    passOneMinute();
    ticket.assign(technician1);
  }

  /**
   * Call init() to set the clock and create technicians
   * Create the test target.
   */
  @BeforeEach
  public void setUp() {
    Clock.setTime(START_TIME);
    ticket = new Ticket(ID, ORIGINATOR, DESCRIPTION, PRIORITY);
    technician1 = Mockito.mock(Technician.class);
    Mockito.when(technician1.getID()).thenReturn(TECHNICIAN1_ID);
    Mockito.when(technician1.getName()).thenReturn(TECHNICIAN1_NAME);
    Mockito.when(technician1.toString()).thenReturn(String.format("Technician %s, %s", TECHNICIAN1_ID, TECHNICIAN1_NAME));
  }

  /**
   * This method tests that all of the ticket's fields were initialized as expected
   */
  @Test
  public void testTicketInitialized(){
    assertThat("ID does not match", ticket.getID(), equalTo(ID));
    assertThat("Originator does not match", ticket.getOriginator(), equalTo(ORIGINATOR));
    assertThat("Description does not match", ticket.getDescription(), equalTo(DESCRIPTION));
    assertThat("Priority does not match", ticket.getPriority(), equalTo(PRIORITY));
    assertThat("Status does not match", ticket.getStatus(), equalTo(Status.CREATED));
    assertThat("Technician assigned when it should not be", ticket.getTechnician(), nullValue());
    assertThat("Unexpected tags added", ticket.getTags().count(), equalTo(0L));
    assertHasEvent(0, Status.CREATED, "Created ticket.");
  }

  /**
   * This method tests that ticket comparison by priority works as expected
   */
  @Test
  public void testCompareTo(){
    Ticket lowPriorityTicket = new Ticket(2, "Andre", "Test ticket with low priority", Priority.LOW);
    Ticket urgentPriorityTicket = new Ticket(3, "Andre", "Test ticket with urgent priority", Priority.URGENT);
    Ticket highPriorityTicket = new Ticket(4, "Andre", "Test ticket with high priority", Priority.HIGH);

    assertThat(ticket, lessThan(lowPriorityTicket));
    assertThat(ticket, greaterThan(urgentPriorityTicket));
    assertThat(ticket.getPriority(), equalTo(highPriorityTicket.getPriority()));
    assertThat(ticket, lessThan(highPriorityTicket));
  }

  /**
   * This method tests that a ticket is assigned as expected
   */
  @Test
  public void testAssignment(){
    assignTicket();
    assertThat(ticket.getStatus(), equalTo(Status.ASSIGNED));
    Mockito.verify(technician1, Mockito.atLeastOnce()).assignTicket(ticket);
  }

  /**
   * This method tests that a ticket is resolved as expected
   */
  @Test
  public void testResolve(){
    assignTicket();
    passOneMinute();
    ticket.resolve("This is a test for resolving tickets");
    assertThat(ticket.getStatus(), equalTo(Status.RESOLVED));
    assertHasEvent(2, Status.RESOLVED, "This is a test for resolving tickets");
    Mockito.verify(technician1, Mockito.atLeastOnce()).resolveTicket(ticket);
  }

  /**
   * This method tests that a ticket is suspended as expected
   */
  @Test
  public void testSuspend(){
    assignTicket();
    passOneMinute();
    ticket.suspend("This is a test for suspending tickets");
    assertThat(ticket.getStatus(), equalTo(Status.WAITING));
    assertHasEvent(2, Status.WAITING, "This is a test for suspending tickets");
  }

  /**
   * This method tests that a ticket is resumed as expected
   */
  @Test
  public void testResume(){
    assignTicket();
    passOneMinute();
    ticket.suspend("This is a test for suspending tickets");
    passOneMinute();
    ticket.resume("This is a test for resuming tickets");
    assertThat(ticket.getStatus(), equalTo(Status.ASSIGNED));
    assertHasEvent(3, Status.ASSIGNED, "This is a test for resuming tickets");
  }

  /**
   * This method tests that notes can be added to a ticket as expected
   */
  @Test
  public void testAddNote(){
    passOneMinute();
    ticket.addNote("This is a test note");
    assertHasEvent(1, null, "This is a test note");
  }

  /**
   * This method tests that Ticket's constructor throws an IllegalArgumentException when it
   * a null value is passed for originator, description, or priority
   */
  @Test
  public void testConstructorNullArgs(){
    Assertions.assertThrows( IllegalArgumentException.class, () -> new Ticket(2, null, "This is a test description", Priority.LOW) );
    Assertions.assertThrows( IllegalArgumentException.class, () -> new Ticket(3, "Andre", null, Priority.LOW) );
    Assertions.assertThrows( IllegalArgumentException.class, () -> new Ticket(4, "Andre", "This is a test description", null) );
    Assertions.assertThrows( IllegalArgumentException.class, () -> new Ticket(5, null, null, null) );
  }

  /**
   * This method tests that Ticket.assign() throws an IllegalArgumentException when
   * a null value is passed
   */
  @Test
  public void testAssignNullArg(){
    passOneMinute();
    Assertions.assertThrows( IllegalArgumentException.class, () -> ticket.assign(null) );
  }

  /**
   * This method tests that Ticket.suspend() throws an IllegalArgumentException when
   * a null value is passed
   */
  @Test
  public void testSuspendNullArg(){
    assignTicket();
    passOneMinute();
    Assertions.assertThrows( IllegalArgumentException.class, () -> ticket.suspend(null) );
  }

  /**
   * This method tests that Ticket.resume() throws an IllegalArgumentException when
   * a null value is passed
   */
  @Test
  public void testResumeNullArg(){
    assignTicket();
    passOneMinute();
    ticket.suspend("This is a test for suspending tickets");
    passOneMinute();
    Assertions.assertThrows( IllegalArgumentException.class, () -> ticket.resume(null) );
  }

  /**
   * This method tests that Ticket.resolve() throws an IllegalArgumentException when
   * a null value is passed
   */
  @Test
  public void testResolveNullArg(){
    assignTicket();
    passOneMinute();
    Assertions.assertThrows( IllegalArgumentException.class, () -> ticket.resolve(null) );
  }

  /**
   * This method tests that Ticket.addNote() throws an IllegalArgumentException when
   * a null value is passed
   */
  @Test
  public void testAddNoteNullArg(){
    passOneMinute();
    Assertions.assertThrows( IllegalArgumentException.class, () -> ticket.addNote(null) );
  }

  /**
   * This method tests that a ticket cannot be assigned after being resolved
   */
  @Test
  public void testCannotAssignAfterResolved(){
    assignTicket();
    passOneMinute();
    ticket.resolve("This is a test for resolving tickets");
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.assign(technician1) );
  }

  /**
   * This method tests that a ticket cannot be suspended after being resolved
   */
  @Test
  public void testCannotSuspendAfterResolved(){
    assignTicket();
    passOneMinute();
    ticket.resolve("This is a test for resolving tickets");
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.suspend("This is a test note") );
  }

  /**
   * This method tests that a ticket cannot be resumed after being resolved
   */
  @Test
  public void testCannotResumeAfterResolved(){
    assignTicket();
    passOneMinute();
    ticket.resolve("This is a test for resolving tickets");
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.resume("This is a test note") );
  }

  /**
   * This method tests that a ticket cannot be resolved after being resolved
   */
  @Test
  public void testCannotResolveAfterResolved(){
    assignTicket();
    passOneMinute();
    ticket.resolve("This is a test for resolving tickets");
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.resolve("This is a test note") );
  }

  /**
   * This method tests that a ticket cannot be resolved when in created status
   */
  @Test
  public void testCannotResolveAfterCreation(){
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.resolve("This is a test message") );
  }

  /**
   * This method tests that a ticket cannot be suspended when in created status
   */
  @Test
  public void testCannotSuspendAfterCreation(){
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.suspend("This is a test message") );
  }

  /**
   * This method tests that a ticket cannot be resumed when in created status
   */
  @Test
  public void testCannotResumeAfterCreation(){
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.resume("This is a test message") );
  }

  /**
   * This method tests that a ticket cannot be resumed when in assigned status
   */
  @Test
  public void testCannotResumeWhenAssigned(){
    assignTicket();
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.resume("This is a test message") );
  }

  /**
   * This method tests that a ticket cannot be suspended when in waiting status
   */
  @Test
  public void testCannotSuspendWhenWaiting(){
    assignTicket();
    passOneMinute();
    ticket.suspend("This is a test for suspending tickets");
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.suspend("This is a test message") );
  }

  /**
   * This method tests that a ticket cannot be assigned when in waiting status
   */
  @Test
  public void testCannotAssignWhenWaiting(){
    assignTicket();
    passOneMinute();
    ticket.suspend("This is a test for suspending tickets");
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.assign(technician1) );
  }

  /**
   * This method tests that a ticket cannot be resolved when in waiting status
   */
  @Test
  public void testCannotResolveWhenWaiting(){
    assignTicket();
    passOneMinute();
    ticket.suspend("This is a test for suspending tickets");
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.resolve("This is a test message") );
  }

  /**
   * This method tests that a ticket reflects tags that have been added
   */
  @Test
  public void testTicketStoresTags(){
    Tag gitHubTag = new Tag("GitHub");
    Tag vpnTag = new Tag("VPN");
    Tag javaTag = new Tag("Java");

    passOneMinute();
    ticket.addTag(gitHubTag);
    passOneMinute();
    ticket.addTag(vpnTag);
    passOneMinute();
    ticket.addTag(javaTag);

    assertThat(ticket.getTags().count(), equalTo(3L));
    assertThat(ticket.getTags().collect(Collectors.toSet()), containsInAnyOrder(gitHubTag, vpnTag, javaTag));
  }

  /**
   * This method tests that a ticket will not list the same tag more than once
   */
  @Test
  public void testTicketMultipleTags(){
    Tag gitHubTag = new Tag("GitHub");
    passOneMinute();
    ticket.addTag(gitHubTag);
    passOneMinute();
    ticket.addTag(gitHubTag);
    passOneMinute();
    ticket.addTag(gitHubTag);
    assertThat(ticket.getTags().count(), equalTo(1L));
  }

  /**
   * This method tests that a ticket will return the correct value when calling
   * Ticket.getMinutesToResolve()
   */
  @Test
  public void testGetMinutesToResolve(){
    assignTicket();
    passOneMinute();
    passOneMinute();
    passOneMinute();
    ticket.resolve("This is a test for resolving tickets");
    assertThat(ticket.getMinutesToResolve(), equalTo(4));
  }

  /**
   * This method tests that a ticket will throw an IllegalStateException when
   * Ticket.getMinutesToResolve() is called on an unresolved ticket
   */
  @Test
  public void testGetMinutesToResolveError(){
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.getMinutesToResolve() );
    assignTicket();
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.getMinutesToResolve() );
    passOneMinute();
    ticket.suspend("This is a test message");
    Assertions.assertThrows( IllegalStateException.class, () -> ticket.getMinutesToResolve() );
  }

  /**
   * This method tests that a ticket can find text in its description
   */
  @Test
  public void testIncludesTextDescription(){
    assertThat(ticket.includesText(DESCRIPTION), equalTo(true));
  }

  /**
   * This method tests that a ticket can find text in its events
   */
  @Test
  public void testIncludesTextEvents(){
    assignTicket();
    passOneMinute();
    ticket.addNote("Cannot connect to the VPN");
    assertThat(ticket.includesText("VPN"), equalTo(true));
    assertThat(ticket.includesText("Assigned"), equalTo(true));
  }
  
}
