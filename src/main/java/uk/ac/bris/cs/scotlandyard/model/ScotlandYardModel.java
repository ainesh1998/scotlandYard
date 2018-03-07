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
	   //Player player1 = players.get(currentPlayer).player();
	   ScotlandYardPlayer player1 = players.get(currentPlayer);
	   player1.player().makeMove(this, player1.location(),new HashSet<>(),this);
	   currentPlayer += 1;


}
   /* private Set<Move> validMove(Colour player){ //Generates all possible moves player can make
	    Node<Integer> position = new Node(getPlayerLocation(player).get());
	    Collection <Edge<Integer,Transport>> edges;
	    Set<Move> moves = new HashSet<>();
	    Set<TicketMove> ticketMoves = new HashSet<>();
	    Set<DoubleMove> doubles = new HashSet<>(); // required for mrX
	    edges = map.getEdgesFrom(position);
	    for(Edge<Integer,Transport> x :edges){ //get all possible ticket moves
	        Ticket ticket = fromTransport(x.data());
	        int destination = x.destination().value();
	        ticketMoves.add(new TicketMove(player, ticket, destination));

	        if(player.isMrX()){ //mrX can also make double Moves
	            Node<Integer> sndPos = x.destination();
	            for(Edge<Integer,Transport> y : map.getEdgesFrom(sndPos)) {
	                Ticket ticket2 = fromTransport(y.data());
	                int destination2 = y.destination().value();
	                doubles.add(new DoubleMove(player,ticket,destination,ticket2,destination2));
                }
            }
        }
        moves.addAll(ticketMoves);
	    moves.addAll(doubles);
	    return moves;
    } */
   private Set<Move> validMove(Colour player){ //Generates all possible moves that can be made from anywhere on the board
     //  Node<Integer> position = new Node(getPlayerLocation(player).get());
       Collection<Edge<Integer,Transport>> edges = map.getEdges();
       Set<Move> moves = new HashSet<>();
       for(Edge<Integer,Transport> e : edges){
           Ticket ticket = fromTransport(e.data());
           int destination = e.destination().value();
           moves.add(new TicketMove(player,ticket,destination));
           if(player.isMrX()){ // mrX can make double moves
               Node<Integer> pos = e.destination();
               for(Edge<Integer,Transport> x: map.getEdgesFrom(pos)){
                   Ticket ticket2 = fromTransport(x.data());
                   int destination2 = x.destination().value();
                   moves.add(new DoubleMove(player,ticket,destination,ticket2,destination2));
               }
           }
       }
       moves.add(new PassMove(player)); //player can also choose not to move
       return moves;
   }

	@Override
    public void accept(Move m) {
	    requireNonNull(m);
	    Set<Move> validMoves = validMove(m.colour());
	    if(!validMoves.contains(m))
	        throw new IllegalArgumentException("illegal move");
	    currentPlayer += 1;

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