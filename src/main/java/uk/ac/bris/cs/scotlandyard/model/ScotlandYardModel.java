package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import sun.security.krb5.SCDynamicStoreConfig;
import sun.security.x509.EDIPartyName;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Node;

import javax.print.attribute.standard.Destination;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>{
	private List<Boolean> rounds;
	private Graph<Integer,Transport> map;
	private PlayerConfiguration mrX, Detective1;
	private PlayerConfiguration[] restOfDetectives;
	private List<ScotlandYardPlayer> players = new ArrayList<>();
	private int currentRound = 0;
	private int currentPlayer;


	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
							 PlayerConfiguration mrX, PlayerConfiguration firstDetective,
							 PlayerConfiguration... restOfTheDetectives) {
		this.rounds = requireNonNull(rounds);
		this.map = requireNonNull(graph);
		this.mrX = requireNonNull(mrX);
		this.Detective1 = requireNonNull(firstDetective);
		this.restOfDetectives = restOfTheDetectives;
		players.add(0,new ScotlandYardPlayer(mrX.player,mrX.colour,mrX.location,mrX.tickets));
		players.add(1, new ScotlandYardPlayer(firstDetective.player,firstDetective.colour,firstDetective.location,firstDetective.tickets));
		currentPlayer = 0;

		if(graph.isEmpty()) //map cannot be empty
			throw new IllegalArgumentException("map should not be empty");
		if(rounds.isEmpty()){
			throw new IllegalArgumentException("Empty Rounds");
		}

		if(mrX.colour != BLACK) // mrX cannot be a detective's colour
			throw new IllegalArgumentException("MrX should be Black");
		if(missingTickets(mrX))
			throw new IllegalArgumentException(("MrX is missing tickets"));

		for( PlayerConfiguration x : restOfTheDetectives){
			checkValidDetective(x);
			players.add(new ScotlandYardPlayer(x.player,x.colour,x.location,x.tickets));
		}

		checkValidDetective(firstDetective);
		checkOverlap(players);
		checkDuplicate(players);
	}

	private void checkOverlap(List<ScotlandYardPlayer> players){
		Set<Integer> locations = new HashSet<>();
		for(ScotlandYardPlayer x : players){
			if(locations.contains(x.location()))
				throw new IllegalArgumentException("2 players are in the same location");
			locations.add(x.location());
		}
	}

	private void checkDuplicate(List<ScotlandYardPlayer> players){
		Set<Colour> locations = new HashSet<>();
		for(ScotlandYardPlayer x : players){
			if(locations.contains(x.colour()))
				throw new IllegalArgumentException("2 players are in the same location");
			locations.add(x.colour());
		}
	}

	private boolean isNotDetective(PlayerConfiguration x){ //checks whether a player is a detective or not
		return (x.colour.isMrX() || x.tickets.get(DOUBLE) != 0|| x.tickets.get(SECRET) != 0);
	}

	private boolean missingTickets(PlayerConfiguration x){
		return !(x.tickets.containsKey(TAXI) && x.tickets.containsKey(BUS) && x.tickets.containsKey(UNDERGROUND) && x.tickets.containsKey(DOUBLE) && x.tickets.containsKey(SECRET) );
	}

	private void checkValidDetective(PlayerConfiguration x){
		requireNonNull(x);
		if(missingTickets(x)) //if a detective doesn't have a ticket type it should fail
			throw new IllegalArgumentException("detective is missing tickets");
		if(isNotDetective(x)) //If there is a mrX in your list of detectives it should fail
			throw new IllegalArgumentException("Only 1 mrX is allowed.");
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

	  ScotlandYardPlayer p = players.get(currentPlayer);
	  p.player().makeMove(this,p.location(),validMove(p.colour()),this);



}

   private Set<Move> validMove(Colour player){ //Generates all possible moves that can be made from anywhere on the board
       ScotlandYardPlayer p = null ;
       for(ScotlandYardPlayer y : players){
           if(y.colour().equals(player)) { //selects correct Player
               p = y;
           }
       }
       Node<Integer> position = new Node(p.location()); //have no idea how to get rid of this warning
       Collection<Edge<Integer,Transport>> edges = map.getEdgesFrom(position);
       Set<Move> moves = new HashSet<>();
       for(Edge<Integer,Transport> e : edges){
           Ticket ticket = fromTransport(e.data());
           int destination = e.destination().value();
           moves.add(new TicketMove(player,ticket,destination)); // Adds all possible ticket Moves
           if(player.isMrX()){ // mrX can make double moves
               Node<Integer> pos = e.destination();
               for(Edge<Integer,Transport> x: map.getEdgesFrom(pos)){ //get edges from 2nd node reached
                   Ticket ticket2 = fromTransport(x.data());
                   int destination2 = x.destination().value();
                   moves.add(new DoubleMove(player,ticket,destination,ticket2,destination2));
               }
           }
       }
       if(moves.size() == 0 && player.isDetective()) // if there's no available place to move
           moves.add(new PassMove(player));        // the player passes if they're a detective
       return moves;
   }

	@Override
    public void accept(Move m) {
	    requireNonNull(m);
	    Set<Move> validMoves = validMove(m.colour());
	    if(!validMoves.contains(m))
	        throw new IllegalArgumentException("illegal move");

       if(m.colour().isMrX()){
           if(m instanceof DoubleMove)
               currentRound += 2;
       }
       if(currentPlayer < getPlayers().size() - 1){
           currentPlayer += 1;
           startRotate();
       }else{
               currentRound += 1;
               currentPlayer = 0; // starts at mrX again

       }

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
		return Collections.unmodifiableSet(new HashSet<>());
	}

	// The location of a player with a given colour in its last known location.
	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		for (ScotlandYardPlayer player : players) {
		    if (player.colour() == colour) {
		        if (player.isMrX()) {
		            if (rounds.get(getCurrentRound())) return Optional.of(player.location());
                    else return Optional.of(0); // if MrX is hidden this round, return 0

                }
		        return Optional.of(player.location());
            }
        }
        return Optional.empty();
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
        for (ScotlandYardPlayer player : players) {
            if (player.colour() == colour) return Optional.of(player.tickets().get(ticket));
        }
        return Optional.empty();
	}

	@Override
	public boolean isGameOver() {
		return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		return players.get(currentPlayer).colour();
	}

	@Override
	public int getCurrentRound() {
		return currentRound;
	}
	public boolean isRevealRound(){
	    return (rounds.get(currentRound));
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