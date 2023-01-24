package com.amica.help;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

public class HelpDeskScenarioTest {
    HelpDesk helpDesk;
    Tags tags;

    public void addNote(int ticketID, String note) {
        helpDesk.getTicketByID(ticketID).addNote(note);
    }

    public void suspend(int ticketID, String reason) {
        helpDesk.getTicketByID(ticketID).suspend(reason);
    }

    public void resume(int ticketID, String reason) {
        helpDesk.getTicketByID(ticketID).resume(reason);
    }

    public void resolve(int ticketID, String reason) {
        helpDesk.getTicketByID(ticketID).resolve(reason);
    }

    /**
     * Initializes tags, sets synonyms in tags, initializes helpDesk
     * based on tags, adds four technicians to helpDesk, and sets up
     * ticket data
     */
    @BeforeEach
    public void setUp(){
        tags = new Tags();
        tags.addSynonym("RDP", "remoting");
        tags.addSynonym("remote desktop", "remoting");
        tags.getTag("CMA");
        tags.getTag("GitHub");
        tags.getTag("VM");
        tags.getTag("VPN");

        helpDesk = new HelpDesk(tags);

        helpDesk.addTechnician("A05589", "Andree", 55491);
        helpDesk.addTechnician("A12312", "Boris", 12399);
        helpDesk.addTechnician("A17440", "Caelem", 34002);
        helpDesk.addTechnician("A20265", "Dineh", 60709);

        Clock.setTime("11/1/21 8:22");
        helpDesk.createTicket("A21013", "Unable to log in.", Ticket.Priority.HIGH);
        Clock.setTime("11/1/21 8:23");
        helpDesk.addTags(1, "remoting");
        Clock.setTime("11/1/21 8:33");
        helpDesk.createTicket("A19556", "Can't connect to remote desktop from my laptop.", Ticket.Priority.HIGH);
        Clock.setTime("11/1/21 8:34");
        helpDesk.addTags(2, "remoting", "laptop");
        Clock.setTime("11/1/21 8:36");
        suspend(2, "Checking if the user can connect from other machines.");
        Clock.setTime("11/1/21 8:37");
        helpDesk.createTicket("A05989", "Need GitHub access.", Ticket.Priority.MEDIUM);
        Clock.setTime("11/1/21 8:38");
        helpDesk.addTags(3, "permissions", "GitHub");
        Clock.setTime("11/1/21 8:39");
        suspend(3, "Requested approval from manager.");
        Clock.setTime("11/1/21 9:05");
        helpDesk.createTicket("T17549", "Can't use just one screen for remote desktop.", Ticket.Priority.MEDIUM);
        Clock.setTime("11/1/21 9:06");
        helpDesk.addTags(4, "remote desktop");
        Clock.setTime("11/1/21 9:07");
        resolve(4, "Explained that this is not a feature we support right now.");
        Clock.setTime("11/1/21 9:48");
        addNote(1, "Determined that it's a VPN problem rather than RDP.");
        Clock.setTime("11/1/21 9:51");
        addNote(1, "Recommended that the user update their browser.");
        Clock.setTime("11/1/21 9:52");
        helpDesk.addTags(1, "VPN");
        Clock.setTime("11/1/21 14:11");
        helpDesk.createTicket("A24490", "Files on my user drive are currupt.", Ticket.Priority.HIGH);
        Clock.setTime("11/1/21 14:12");
        helpDesk.addTags(5, "VM");
        Clock.setTime("11/1/21 14:14");
        resume(2, "User: Yes, I can connect from other desktop machines at Amica.");
        Clock.setTime("11/1/21 14:17");
        suspend(5, "Requested examples of corrupt files.");
        Clock.setTime("11/1/21 16:39");
        helpDesk.createTicket("T24090", "Need CMA access.", Ticket.Priority.MEDIUM);
        Clock.setTime("11/1/21 16:41");
        helpDesk.addTags(6, "Permissions", "CMA");
        Clock.setTime("11/1/21 16:42");
        suspend(6, "Requested approval from manager.");

        Clock.setTime("11/2/21 8:11");
        helpDesk.createTicket("A15711", "Laptop won't start up.", Ticket.Priority.URGENT);
        Clock.setTime("11/2/21 8:12");
        helpDesk.addTags(7, "laptop");
        Clock.setTime("11/2/21 8:45");
        resume(6, "Received approval.");
        resolve(6, "Added permission.");
        Clock.setTime("11/2/21 8:52");
        helpDesk.createTicket("A20271", "Can't login.", Ticket.Priority.HIGH);
        Clock.setTime("11/2/21 8:53");
        helpDesk.addTags(8, "remoting");
        Clock.setTime("11/2/21 10:19");
        helpDesk.createTicket("T13370", "Need to reset MobilePass.", Ticket.Priority.HIGH);
        Clock.setTime("11/2/21 10:20");
        resume(3, "Received approval.");
        resolve(3, "Added permission.");
        Clock.setTime("11/2/21 10:21");
        helpDesk.addTags(9, "vpn");
        Clock.setTime("11/2/21 10:22");
        suspend(9, "Tried to contact user; left voice mail.");
        Clock.setTime("11/2/21 11:00");
        helpDesk.createTicket("A14401", "Unable to log in.", Ticket.Priority.HIGH);
        Clock.setTime("11/2/21 11:01");
        helpDesk.addTags(10, "RDP");
        Clock.setTime("11/2/21 11:32");
        helpDesk.createTicket("T11918",
                "No disk space left! I don't have that much stuff on here; not sure what's taking up all the space.",
                Ticket.Priority.URGENT);
        Clock.setTime("11/2/21 11:33");
        helpDesk.addTags(11, "vm");
        Clock.setTime("11/2/21 14:49");
        resolve(1, "User reports that the browser update fixed it.");

        Clock.setTime("11/3/21 9:22");
        helpDesk.createTicket("A13288", "Need GitHub access.", Ticket.Priority.MEDIUM);
        Clock.setTime("11/3/21 9:23");
        helpDesk.addTags(12, "permissions", "github");
        Clock.setTime("11/3/21 9:24");
        suspend(12, "Requested approval from manager.");
        Clock.setTime("11/3/21 11:11");
        helpDesk.createTicket("A22465", "Laptop audio seems to be broken.", Ticket.Priority.MEDIUM);
        Clock.setTime("11/3/21 11:12");
        helpDesk.addTags(13, "laptop", "audio");
        Clock.setTime("11/3/21 11:39");
        helpDesk.createTicket("A18087", "Can't log in.", Ticket.Priority.HIGH);
        Clock.setTime("11/3/21 11:40");
        helpDesk.addTags(14, "remote desktop");
        Clock.setTime("11/3/21 13:11");
        resolve(10, "Opened remote access to RI150WS3344; confirmed user can connect.");
        Clock.setTime("11/3/21 13:16");
        resume(5, "User: See /Users/A10551/Projects/Spec_20211015.pdf.");
        Clock.setTime("11/3/21 13:17");
        addNote(5, "Building a new VM.");
        Clock.setTime("11/3/21 13:18");
        resolve(5, "Migrated most files to new VM, restored remaining files from backups, switched IP address over.");
        Clock.setTime("11/3/21 13:19");
        resolve(11, "Found user's ME2020 Maven cache way overloaded, recommended cleaning it out.");
    }

    public static void assertTrue(boolean condition, String error) {
        assertThat(error, condition, equalTo(true));
    }

    public static void assertEqual(Object actual, Object expected, String error) {
        assertThat(String.format(error, actual), actual, equalTo(expected));
    }

    public static void assertCount(Stream<?> results,
                                   long expectedCount, String error) {
        assertEqual(results.count(), expectedCount, error);
    }

    public static long getCount(Stream<?> stream) {
        return stream.count();
    }

    /**
     * This method tests that the ijmplementation meets the following requirements.
     * All of the test logic uses the {@link HelpDeskAPI} and so is pre-written.
     * <ul>
     * <li>We can find a ticket by ID.</li>
     * <li>IDs are a generated sequence starting at 1.</li>
     * <li>We can find all tickets in a given status.</li>
     * <li>We can find all tickets not in a given status.</li>
     * <li>Tickets are automatically assigned after being created.</li>
     * <li>Tickets show the RESOLVED status after being resolved.</li>
     * </ul>
     */
    @Test
    public void test1_Tickets(){
        assertTrue(helpDesk.getTicketByID(0) == null, "There shouldn't be a ticket 0.");
        assertTrue(helpDesk.getTicketByID(1) != null, "There should be a ticket 1.");
        assertTrue(helpDesk.getTicketByID(14) != null, "There should be a ticket 14.");

        assertEqual(helpDesk.getTicketByID(1).getStatus(), Ticket.Status.RESOLVED,
                "Ticket 1 should be RESOLVED, was %s.");
        assertEqual(helpDesk.getTicketByID(12).getStatus(), Ticket.Status.WAITING,
                "Ticket 12 should be WAITING, was %s.");

        assertCount(helpDesk.getTicketsByStatus(Ticket.Status.CREATED), 0,
                "There shuldn't be any tickets in the CREATED state, was %d.");
        assertCount(helpDesk.getTicketsByStatus(Ticket.Status.WAITING), 2,
                "There shuld be 2 tickets in the WAITING state, was %d.");
        assertCount(helpDesk.getTicketsByStatus(Ticket.Status.RESOLVED), 7,
                "There shuld be 7 tickets in the RESOLVED state, was %d.");
    }

    /**
     * This method tests that the ijmplementation meets the following requirements.
     * You will have to fill out the test logic yourself, based on your design,
     * class names, method names, etc.
     * <ul>
     * <li>We can't re-assign a resolved ticket.</li>
     * <li>Each ticket has a history that includes events for assignment and
     * resolution, with timestamps taken from the {@link Clock}.</li>
     * <li>Notes added to the ticket appear in the event history as well.</li>
     * </ul>
     */
    @Test
    public void test2_History() {
        Iterator<Event> history = helpDesk.getTicketByID(4).getHistory().iterator();
        Event created4 = history.next();
        assertEqual(Clock.format(created4.getTimestamp()), "11/1/21 9:05",
                "Ticket 4 should have been created at 9:05, was %s.");
        assertEqual(created4.getNewStatus(), Ticket.Status.CREATED, "Ticket 4's first event should be CREATED, was %s.");
        assertEqual(created4.getNote(), "Created ticket.", "Ticket 4 creation note is wrong: %s.");
        Event assigned4 = history.next();
        assertEqual(Clock.format(assigned4.getTimestamp()), "11/1/21 9:05",
                "Ticket 4 should have been assigned at 9:05, was %s.");
        assertEqual(assigned4.getNewStatus(), Ticket.Status.ASSIGNED, "Ticket 4's second event should be ASSIGNED, was %s.");
        assertEqual(assigned4.getNote(), "Assigned to Technician A20265, Dineh.",
                "Ticket 4 assignment note is wrong: %s.");
        Event resolved4 = history.next();
        assertEqual(Clock.format(resolved4.getTimestamp()), "11/1/21 9:07",
                "Ticket 4 should have been resolved at 9:07, was %s.");
        assertEqual(resolved4.getNewStatus(), Ticket.Status.RESOLVED, "Ticket 4's second event should be RESOLVED, was %s.");
        assertEqual(resolved4.getNote(), "Explained that this is not a feature we support right now.",
                "Ticket 4 resolution note is wrong: %s.");

        history = helpDesk.getTicketByID(2).getHistory().iterator();
        history.next();
        history.next();
        history.next();
        Event note7 = history.next();
        assertEqual(Clock.format(note7.getTimestamp()), "11/1/21 14:14",
                "Ticket 2's 2nd note should be stamped 14:14, was %s.");
        assertTrue(note7.getNewStatus() == Ticket.Status.ASSIGNED,
                "Ticket 2's second note status should be ASSIGNED, was " + note7.getNewStatus() + ".");
        assertEqual(note7.getNote(), "User: Yes, I can connect from other desktop machines at Amica.",
                "Ticket 2's 2nd note is wrong: %s.");
    }

    /**
     * This method tests that the ijmplementation meets the following requirements.
     * Some of this is pre-written, and you will have to develop the rest.
     * <ul>
     * <li>We can find all tickets assigned to a given technician.</li>
     * <li>Tickets are always assigned to the technician with the fewest active
     * (i.e. unresolved) tickets.</li>
     * <li>Tickets are sorted from highest priority to lowest, in the master data
     * set and for each technician.</li>
     * </ul>
     */
    @Test
    public void test3_Assignment() {
        assertCount(helpDesk.getTicketsByTechnician("A05589"), 5,
                "Andree should have been assigned 5 tickets, but has %s.");
        assertCount(helpDesk.getTicketsByTechnician("A12312"), 3,
                "Boris should have been assigned 3 tickets, but has %s.");
        assertCount(helpDesk.getTicketsByTechnician("A17440"), 3,
                "Caelem should have been assigned 3 tickets, but has %s.");
        assertCount(helpDesk.getTicketsByTechnician("A20265"), 3,
                "Dineh should have been assigned 3 tickets, but has %s.");

        Iterator<Ticket> tickets = ((HelpDesk) helpDesk).getTickets().iterator();
        assertEqual(tickets.next().getID(), 7, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 11, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 1, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 2, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 5, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 8, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 9, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 10, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 14, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 3, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 4, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 6, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 12, "Out of sequence in master set: %s");
        assertEqual(tickets.next().getID(), 13, "Out of sequence in master set: %s");

        Iterator<Technician> techs = ((HelpDesk) helpDesk).getTechnicians().iterator();

        tickets = techs.next().getActiveTickets().iterator();
        assertEqual(tickets.next().getID(), 8, "Out of sequence for Andree: %s");
        assertEqual(tickets.next().getID(), 12, "Out of sequence for Andree: %s");
        assertEqual(tickets.next().getID(), 13, "Out of sequence for Andree: %s");
        assertTrue(!tickets.hasNext(), "Andree should have 3 active tickets, was. %s");

        tickets = techs.next().getActiveTickets().iterator();
        assertEqual(tickets.next().getID(), 7, "Out of sequence for Boris: %s");
        assertEqual(tickets.next().getID(), 2, "Out of sequence for Boris: %s");
        assertEqual(tickets.next().getID(), 14, "Out of sequence for Boris: %s");
        assertTrue(!tickets.hasNext(), "Boris should have 3 active tickets, was %s.");

        tickets = techs.next().getActiveTickets().iterator();
        assertEqual(tickets.next().getID(), 9, "Out of sequence for Caelem: %s");
        assertTrue(!tickets.hasNext(), "Caelem should have 1 active tickets, was %s.");

        tickets = techs.next().getActiveTickets().iterator();
        assertTrue(!tickets.hasNext(), "Dineh should have 0 active tickets, was %s.");
    }

    /**
     * This method tests that the ijmplementation meets the following requirements.
     * You will have to fill out the test logic yourself, based on your design,
     * class names, method names, etc.
     * <ul>
     * <li>We can tag tickets and find them by tags.</li>
     * <li>A ticket will only appear once in these results, even if it has multiple
     * requested tags.</li>
     * <li>Tags are considered equal on a case-insensitive basis.</li>
     * </ul>
     */
    @Test
    public void test4_Tags() {
        assertCount(helpDesk.getTicketsWithAnyTag("laptop"), 3,
                "There should be 3 tickets with the 'laptop' tag, was %s.");

        assertCount(helpDesk.getTicketsWithAnyTag("VM"), 2,
                "There should be 2 tickets with the 'vm' tag, was %s.");

        assertCount(helpDesk.getTicketsWithAnyTag("permissions", "CMA"), 3,
                "There should be 3 tickets with the 'permissions' and/or 'CMA' tags, was %s.");
    }

    /**
     * This method tests that the ijmplementation meets the following requirements.
     * All of the test logic uses the {@link HelpDeskAPI} and so is pre-written.
     * <ul>
     * <li>We can correctly calculate our average time to resolve a ticket.</li>
     * <li>We can also see the average time per technician.</li>
     * </ul>
     */
    @Test
    public void test5_TimeToResolve() {
        int minutes = helpDesk.getAverageMinutesToResolve();
        int hours = minutes / 60;
        minutes %= 60;
        assertEqual(hours, 24, "Average hours to resolve should be 24, was %s.");
        assertEqual(minutes, 29, "Average minutes to resolve should be 29, was %s.");

        Map<String, ? extends Number> byTech = helpDesk.getAverageMinutesToResolvePerTechnician();
        assertEqual(byTech.get("A05589").intValue(), 1396, "Andree's average should be 1396, was %s.");
        assertTrue(!byTech.containsKey("A12312"), "Boris shouldn't have an average time to resolve.");
        assertEqual(byTech.get("A17440").intValue(), 1557, "Caelem average should be 1557, was %s.");
        assertEqual(byTech.get("A20265").intValue(), 1458, "Dineh's average should be 1458, was %s.");
    }

    /**
     * This method tests that the ijmplementation meets the following requirements.
     * All of the test logic uses the {@link HelpDeskAPI} and so is pre-written.
     * <ul>
     * <li>We can find tickets whose descriptions or notes include a given
     * substring.</li>
     * </ul>
     */
    @Test
    public void test6_TextSearch() {
        assertCount(helpDesk.getTicketsByText("corrupt"), 1,
                "There should be one ticket with the text 'corrupt', was %s.");
        assertCount(helpDesk.getTicketsByText("browser"), 1,
                "There should be one ticket with the text 'browser', was %s.");
    }

    /**
     * This method tests that the ijmplementation meets the following requirements.
     * Some of this is pre-written, and you will have to develop the rest.
     * <ul>
     * <li>We can reopen a resolved ticket, with a new reason and priority.</li>
     * <li>We can't reopen an un-resolved ticket.</li>
     * <li>Reopened tickets take on the originator of the prior ticket, and are
     * assigned to the original technician.</li>
     * <li>Reopened tickets compile their own history and that of the prior
     * ticket.</li>
     * <li>Reopened tickets compile their own tags and those of the prior
     * ticket.</li>
     * <li>Reopened tags search their own and the prior ticket's text.</li>
     */
    @Test
    public void test7_ReopenedTickets() {
        Clock.setTime("11/3/21 14:01");
        helpDesk.reopenTicket(6, "Still can't connect.", Ticket.Priority.MEDIUM);
        Clock.setTime("11/3/21 14:12");
        helpDesk.reopenTicket(3, "Still can't log in.", Ticket.Priority.HIGH);
        Clock.setTime("11/3/21 14:59");
        helpDesk.addTags(3, "VPN");

        long expectedSize = getCount(helpDesk.getTicketByID(6).getHistory()) + 2;
        long actualSize = getCount(helpDesk.getTicketByID(15).getHistory());
        assertEqual(actualSize, expectedSize,
                "Reopened ticket should have " + expectedSize + " events, was %s");

        assertTrue(helpDesk.getTicketsWithAnyTag("GitHub")
                        .anyMatch(helpDesk.getTicketByID(16)::equals),
                "Reopened ticket not found by prior ticket's tag;");
        assertTrue(helpDesk.getTicketsWithAnyTag("VPN")
                        .anyMatch(helpDesk.getTicketByID(16)::equals),
                "Reopened ticket not found by its own tag;");

        assertTrue(helpDesk.getTicketsByText("access")
                        .anyMatch(helpDesk.getTicketByID(15)::equals),
                "Reopened ticked should be found by original description.");
    }

    /**
     * This method tests that the ijmplementation meets the following requirements.
     * You will have to fill out the test logic yourself, based on your design,
     * class names, method names, etc.
     * <ul>
     * <li>We can pre-define synonyms for tags, and see them consolidated to a
     * chosen master tag.</li>
     * <li>We can pre-define preferred capitalization for tags, instead of having
     * everything pushed to lower case.</li>
     * </ul>
     * This test relies on additional setup, to be performed in the
     * {@link #runSimulation runSimulation} method, before creating and tagging
     * tickets:
     * <ul>
     * <li>Create synonyms for "remoting": "RDP" and "remote desktop".</li>
     * <li>Pre-set the following capitalizations: "VM", "VPN", "CMA", and
     * "GitHub".</li>
     * </ul>
     */
    @Test
    public void test8_Synonyms() {
        assertCount(helpDesk.getTicketsWithAnyTag("remoting"), 6,
                "There should be 6 tickets with the 'remoting' tag, was %s.");

        assertEqual(tags.getTag("github").getValue(), "GitHub", "The tag capitalization GitHub should be used, was %s");

        Iterator<Tag> allTags = tags.getTags().iterator();
        assertEqual(allTags.next().getValue(), "audio", "Unexpected tag: %s.");
        assertEqual(allTags.next().getValue(), "CMA", "Unexpected tag: %s.");
        assertEqual(allTags.next().getValue(), "GitHub", "Unexpected tag: %s.");
        assertEqual(allTags.next().getValue(), "laptop", "Unexpected tag: %s.");
        assertEqual(allTags.next().getValue(), "permissions", "Unexpected tag: %s.");
        assertEqual(allTags.next().getValue(), "remoting", "Unexpected tag: %s.");
        assertEqual(allTags.next().getValue(), "VM", "Unexpected tag: %s.");
        assertEqual(allTags.next().getValue(), "VPN", "Unexpected tag: %s.");
    }

    /**
     * Tests the latest-activity query.
     */
    @Test
    public void test9_LatestActivity() {
        assertCount(helpDesk.getLatestActivity(10)
                        .filter(e -> e.getTicketID() == 5), 3,
                "3 of the events in the latest 10 should relate to ticket 5; was %d.");
    }

}
