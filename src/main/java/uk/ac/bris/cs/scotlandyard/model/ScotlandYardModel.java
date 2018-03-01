package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.DOUBLE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.SECRET;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {
	private List<Boolean> rounds;
	private Graph<Integer,Transport> map;
	private PlayerConfiguration mrX, Detective1;
	private PlayerConfiguration[] restOfDetectives;
	private List<ScotlandYardPlayer> players;
	private List<PlayerConfiguration> testlist; //simply used to make iteration easier


	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {
		this.rounds = requireNonNull(rounds);
		this.map = requireNonNull(graph);
		this.mrX = requireNonNull(mrX);
		this.Detective1 = requireNonNull(firstDetective);
		this.restOfDetectives = restOfTheDetectives;
		if(graph.isEmpty()) //map cannot be empty
		    throw new IllegalArgumentException("map should not be empty");

		if(mrX.colour != BLACK) // mrX cannot be a detective's colour
            throw new IllegalArgumentException("MrX should be Black");

		for( PlayerConfiguration x : restOfTheDetectives){
			checkValidDetective(x);
			players.add(new ScotlandYardPlayer(x.player,x.colour,x.location,x.tickets));
			testlist.add(x);
		}

       checkValidDetective(firstDetective);
        testlist.add(firstDetective);
        testlist.add(mrX);
       checkOverlap(testlist);
       checkDuplicate(testlist);
	}
	private void checkOverlap(List<PlayerConfiguration> players){
	    Set<Integer> locations = new HashSet<>();
	    for(PlayerConfiguration x : players){
            if(locations.contains(x.location))
                throw new IllegalArgumentException("2 players are in the same location");
            locations.add(x.location);
        }
    }
    private void checkDuplicate(List<PlayerConfiguration> players){
        Set<Colour> locations = new HashSet<>();
        for(PlayerConfiguration x : players){
            if(locations.contains(x.colour))
                throw new IllegalArgumentException("2 players are in the same location");
            locations.add(x.colour);
        }
    }

	private boolean isNotDetective(PlayerConfiguration x){ //checks whether a player is a detective or not
		return (x.colour.isMrX() || x.tickets.containsKey(DOUBLE)|| x.tickets.containsKey(SECRET));
	}

	private boolean missingTickets(PlayerConfiguration x){ // check that detectives start with the right amount of cards
	    return !(x.tickets.containsKey(Ticket.TAXI) && x.tickets.containsKey(Ticket.BUS) && x.tickets.containsKey(Ticket.UNDERGROUND));
    }

    private void checkValidDetective(PlayerConfiguration x){
		requireNonNull(x);
        if(isNotDetective(x)) //If there is a mrX in your list of detectives it should fail
            throw new IllegalArgumentException("Only 1 mrX is allowed.");
        if(missingTickets(x)) //if a detective doesn't have a ticket type it should fail
            throw new IllegalArgumentException("detective is missing tickets");
    }

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		// TODO
		List<Colour> colours = new ArrayList<>();
		for ( ScotlandYardPlayer x: players){
		    colours.add(x.colour());
        }
        return Collections.unmodifiableList(colours);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public boolean isGameOver() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Colour getCurrentPlayer() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public int getCurrentRound() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Boolean> getRounds() {
		// TODO
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		// TODO
		return new ImmutableGraph<>(map);
	}

}
