package com.amica.help;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.amica.help.Ticket.Priority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for the {@link HelpDesk} class.
 * 
 * @author Will Provost
 */
public class HelpDeskTest {

	public static final String TECH1 = "TECH1";
	public static final String TECH2 = "TECH2";
	public static final String TECH3 = "TECH3";

	public static final int TICKET1_ID = 1;
	public static final String TICKET1_ORIGINATOR = "TICKET1_ORIGINATOR";
	public static final String TICKET1_DESCRIPTION = "TICKET1_DESCRIPTION";
	public static final Priority TICKET1_PRIORITY = Priority.LOW;
	public static final int TICKET2_ID = 2;
	public static final String TICKET2_ORIGINATOR = "TICKET2_ORIGINATOR";
	public static final String TICKET2_DESCRIPTION = "TICKET2_DESCRIPTION";
	public static final Priority TICKET2_PRIORITY = Priority.HIGH;
	
	public static final String TAG1 = "TAG1";
	public static final String TAG2 = "TAG2";
	public static final String TAG3 = "TAG3";
	
	private HelpDesk helpDesk = new HelpDesk();
	private Technician tech1;
	private Technician tech2;
	private Technician tech3;

	private int currentExtension = 12345;

	/**
	 * Custom matcher that checks the contents of a stream of tickets
	 * against expected IDs, in exact order;
	 */
	public static class HasIDs extends TypeSafeMatcher<Stream<? extends Ticket>> {

		private String expected;
		private String was;
		
		public HasIDs(int... IDs) {
			int[] expectedIDs = IDs;
			expected = Arrays.stream(expectedIDs)
					.mapToObj(Integer::toString)
					.collect(Collectors.joining(", ", "[ ", " ]"));		
		}
		
		public void describeTo(Description description) {
			
			description.appendText("tickets with IDs ");
			description.appendText(expected);
		}
		
		@Override
		public void describeMismatchSafely
				(Stream<? extends Ticket> tickets, Description description) {
			description.appendText("was: tickets with IDs ");
			description.appendText(was);
		}

		protected boolean matchesSafely(Stream<? extends Ticket> tickets) {
			was = tickets.mapToInt(Ticket::getID)
					.mapToObj(Integer::toString)
					.collect(Collectors.joining(", ", "[ ", " ]"));
			return expected.equals(was);
		}
		
	}
	public static Matcher<Stream<? extends Ticket>> hasIDs(int... IDs) {
		return new HasIDs(IDs);
	}

	@BeforeEach
	public void setUp(){
		helpDesk.addTechnician(TECH1, TECH1, currentExtension);
		currentExtension += 1;
		helpDesk.addTechnician(TECH2, TECH2, currentExtension);
		currentExtension += 1;
		helpDesk.addTechnician(TECH3, TECH3, currentExtension);
		currentExtension += 1;

		Iterator<Technician> iterator = helpDesk.getTechnicians().iterator();
		tech1 = iterator.next();
		tech2 = iterator.next();
		tech3 = iterator.next();

		Clock.setTime(100);
	}

	public void createTicket1(){
		helpDesk.createTicket(TICKET1_ORIGINATOR, TICKET1_DESCRIPTION, TICKET1_PRIORITY);
	}

	public void createTicket2(){
		helpDesk.createTicket(TICKET2_ORIGINATOR, TICKET2_DESCRIPTION, TICKET2_PRIORITY);
	}

	@Test
	public void testTicketCreation(){
		createTicket1();
		createTicket2();
		assertThat(helpDesk.getTicketByID(TICKET2_ID), hasProperty("description", equalTo(TICKET2_DESCRIPTION)));
	}

	@Test
	public void testNoAddedTickets(){
		assertThat(helpDesk.getTicketByID(0), nullValue());
	}

	@Test
	public void testNoAddedTechnicians(){
		HelpDesk localHelpDesk = new HelpDesk();
		Assertions.assertThrows(IllegalStateException.class, () -> localHelpDesk.createTicket(TICKET1_ORIGINATOR, TICKET1_DESCRIPTION, TICKET1_PRIORITY));
	}

	@Test
	public void testAutoTicketAssignment(){
		createTicket1();
		assertThat(helpDesk.getTicketByID(TICKET1_ID), hasProperty("technician", equalTo(tech1)));
		assertThat(tech1.getActiveTickets().count(), equalTo(1L));
	}

	@Test
	public void testManualTicketAssignment(){
		createTicket1();
		tech2.assignTicket(helpDesk.getTicketByID(TICKET1_ID));
		assertThat(tech2.getActiveTickets().findFirst().get(), equalTo(helpDesk.getTicketByID(TICKET1_ID)));
		createTicket2();
		tech3.assignTicket(helpDesk.getTicketByID(TICKET2_ID));
		assertThat(tech3.getActiveTickets().findFirst().get(), equalTo(helpDesk.getTicketByID(TICKET2_ID)));
	}

	@Test
	public void testGetTicketsByStatus(){
		createTicket1();
		createTicket2();
		helpDesk.getTicketByID(TICKET2_ID).resolve("This is a test resolution");
		assertThat(helpDesk.getTicketsByStatus(Ticket.Status.ASSIGNED).count(), equalTo(1L));
		assertThat(helpDesk.getTicketsByStatus(Ticket.Status.RESOLVED).count(), equalTo(1L));
	}

	@Test
	public void testGetTicketsByNotStatus(){
		createTicket1();
		createTicket2();
		assertThat(helpDesk.getTicketsByNotStatus(Ticket.Status.WAITING), hasIDs(TICKET2_ID, TICKET1_ID));
	}

	@Test
	public void testGetTicketsWithAnyTag(){
		createTicket1();
		helpDesk.addTags(TICKET1_ID, TAG3, TAG1);
		createTicket2();
		helpDesk.addTags(TICKET2_ID, TAG2, TAG3);
		assertThat(helpDesk.getTicketsWithAnyTag(TAG1), hasIDs(TICKET1_ID));
		assertThat(helpDesk.getTicketsWithAnyTag(TAG2), hasIDs(TICKET2_ID));
		assertThat(helpDesk.getTicketsWithAnyTag(TAG3), hasIDs(TICKET2_ID, TICKET1_ID));
	}

	@Test
	public void testGetTicketsByTechnician(){
		createTicket1();
		createTicket2();
		assertThat(helpDesk.getTicketsByTechnician(TECH1), hasIDs(TICKET1_ID));
		assertThat(helpDesk.getTicketsByTechnician(TECH2), hasIDs(TICKET2_ID));
	}

	@Test
	public void testGetTicketsByText(){
		createTicket1();
		createTicket2();
		helpDesk.getTicketByID(TICKET1_ID).addNote("This is a test note");
		helpDesk.getTicketByID(TICKET2_ID).addNote("This another is a test note");
		assertThat(helpDesk.getTicketsByText(TICKET1_DESCRIPTION), hasIDs(TICKET1_ID));
		assertThat(helpDesk.getTicketsByText("note"), hasIDs(TICKET2_ID, TICKET1_ID));
	}

// Step5 uses a generic stream matcher:
//	public static Matcher<Stream<? extends Ticket>> hasIDs(Integer... IDs) {
//		return HasKeys.hasKeys(Ticket::getID, IDs);
//	}
}

