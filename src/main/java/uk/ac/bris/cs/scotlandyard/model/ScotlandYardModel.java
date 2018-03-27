package uk.ac.bris.cs.scotlandyard.model;


import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Node;


// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor{
	private List<Boolean> rounds;
	private Graph<Integer,Transport> map;
	private List<ScotlandYardPlayer> players = new ArrayList<>();
	private int currentRound = 0;
	private int currentPlayer;
	private boolean gameOver = false;
	private int xLastLocation = 0;
	private int xActualLocation;
	private boolean revealRound = false;
	private boolean gameNotStarted = true;
	private List<Spectator> spectators = new ArrayList<>();
	private Set<Colour> winningPlayers = new HashSet<>();

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
							 PlayerConfiguration mrX, PlayerConfiguration firstDetective,
							 PlayerConfiguration... restOfTheDetectives) {
		this.rounds = requireNonNull(rounds);
		this.map = requireNonNull(graph);
		requireNonNull(mrX);
		requireNonNull(firstDetective);
		xActualLocation = mrX.location;
		players.add(0,new ScotlandYardPlayer(mrX.player,mrX.colour,mrX.location,mrX.tickets));
		players.add(1, new ScotlandYardPlayer(firstDetective.player,firstDetective.colour,firstDetective.location,firstDetective.tickets));
		currentPlayer = 0;
		if(graph.isEmpty()) //map cannot be empty
			throw new IllegalArgumentException("map should not be empty");
		if(rounds.isEmpty()){
			throw new IllegalArgumentException("Empty Rounds");
		}
		checkValidMrx(mrX);
		for( PlayerConfiguration x : restOfTheDetectives){
			checkValidDetective(x);
			players.add(new ScotlandYardPlayer(x.player,x.colour,x.location,x.tickets));
		}

		checkValidDetective(firstDetective);
		checkOverlap(players);
		checkDuplicate(players);
		updateGameOver();
	}

	private void checkValidMrx(PlayerConfiguration mrX){
        if(mrX.colour != BLACK) // mrX cannot be a detective's colour
            throw new IllegalArgumentException("MrX should be Black");
        if(missingTickets(mrX))
            throw new IllegalArgumentException(("MrX is missing tickets"));
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
        if(spectators.contains(spectator))
            throw new IllegalArgumentException("spectator already registered");
        spectators.add(requireNonNull(spectator));
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
        requireNonNull(spectator);
        if(!spectators.contains(spectator))
            throw new IllegalArgumentException("spectator wasn't registered");
        spectators.remove(requireNonNull(spectator));
	}

	@Override
	public void startRotate() {
		gameNotStarted = false;
		if(gameOver)
			throw new IllegalStateException();
	   ScotlandYardPlayer p = players.get(0);
	   p.player().makeMove(this,xActualLocation,validMove(p.colour()),this);
	}

	private void takeMove(){
        ScotlandYardPlayer p = players.get(currentPlayer);
        p.player().makeMove(this,p.location(),validMove(p.colour()),this);
    }

	private Set<Move> getTicketMoves(Edge<Integer,Transport> e,Set<Move> moves,Colour player){
		Ticket ticket = fromTransport(e.data());
		int destination = e.destination().value();
        Boolean noMoreRounds = (getCurrentRound() == rounds.size() - 1 );
        if(!getDetectiveLocations().contains(destination)){
            if(getScotPlayer(player).hasTickets(ticket))
                moves.add(new TicketMove(player,ticket,destination));
			if(player.isMrX() && getScotPlayer(player).hasTickets(SECRET))
				moves.add(new TicketMove(player,SECRET,destination));
		}
		if(player.isMrX() && getScotPlayer(player).hasTickets(DOUBLE) && !noMoreRounds){
			Node<Integer> pos = e.destination();
			for(Edge<Integer,Transport> x: map.getEdgesFrom(pos)){
				moves.addAll(getDoubleMoves(x,moves,player,ticket,destination));
			}
		}
		return moves;
	}
	private Set<Move> getDoubleMoves(Edge<Integer,Transport> e,Set<Move> moves,Colour player,Ticket ticket,int destination){
		Ticket ticket2 = fromTransport(e.data());
		int destination2 = e.destination().value();
		Boolean sameTickets = (ticket.equals(ticket2) && getScotPlayer(player).tickets().get(ticket) < 2 );
		if(!getDetectiveLocations().contains(destination) && !getDetectiveLocations().contains(destination2) && getScotPlayer(player).hasTickets(ticket2)) {
            if (getScotPlayer(player).hasTickets(SECRET)) {
                moves.add(new DoubleMove(player, ticket, destination, SECRET, destination2));
                moves.add(new DoubleMove(player, SECRET, destination, ticket2, destination2));
            }
            if (getScotPlayer(player).tickets().get(SECRET) >=2) {
                moves.add(new DoubleMove(player, SECRET, destination, SECRET, destination2));
            }
		    if(!sameTickets){
				moves.add(new DoubleMove(player, ticket, destination, ticket2, destination2));
            }
		}
		return moves;
	}

   private Set<Move> validMove(Colour player){ //Generates all possible moves that can be made from anywhere on the board
       ScotlandYardPlayer p = getScotPlayer(player);
       Node<Integer> position = p.isMrX()? new Node<>(xActualLocation):new Node<>(p.location()); //have no idea how to get rid of this warning
       Collection<Edge<Integer,Transport>> edges = map.getEdgesFrom(position);
       Set<Move> moves = new HashSet<>();
       for(Edge<Integer,Transport> e : edges){
           moves = getTicketMoves(e,moves,player);
       }
        // if there's no available place to move for a detective
       if(player.isDetective() && moves.size() == 0)
           moves.add(new PassMove(player));        // the player passes if they're a detective
       return moves;
   }

   private List<Integer> getDetectiveLocations(){
		List<Integer> locations = new ArrayList<>();
		for(ScotlandYardPlayer p: players){
			if(p.isDetective())
				locations.add(p.location());
		}
		return locations;
   }

   private ScotlandYardPlayer getScotPlayer(Colour colour){
		for(ScotlandYardPlayer x : players){
			if(x.colour().equals(colour)){
				return x;
			}
		}
		throw new IllegalArgumentException("colour not in list");
   }

   //Visit method implementations from MoveVisitor
    @Override
	public void visit(PassMove m) {
		//passMove does not do anything
	}

	@Override
	public void visit(TicketMove m) {
		ScotlandYardPlayer p = getScotPlayer(m.colour());
		ScotlandYardPlayer mrX = getScotPlayer(BLACK);
		p.removeTicket(m.ticket());
		if(p.isDetective()) {
            p.location(m.destination());
            mrX.addTicket(m.ticket());
        }
		else {
            xActualLocation = m.destination();//stores where mrX actually is
            currentRound += 1; //if mrX increment round
        }
	}

	@Override
	public void visit(DoubleMove m) {
		ScotlandYardPlayer p = getScotPlayer(m.colour());
		 //if they're no spectators just decrement the tickets and increment the round
		if(spectators.isEmpty()){
			p.removeTicket(DOUBLE);
			p.removeTicket(m.firstMove().ticket());
			p.removeTicket(m.secondMove().ticket());
			currentRound += 2;

		}else{
			updateDoubleSpec(m,p);
		}
        xActualLocation = m.finalDestination();
	}

	@Override
    public void accept(Move m) {
            requireNonNull(m);
            Set<Move> validMoves = validMove(m.colour());
            revealRound = players.get(currentPlayer).isMrX() && rounds.get(currentRound);

            if(!validMoves.contains(m))
                throw new IllegalArgumentException("illegal move");

			m.visit(this);
			updateGameOver();
			currentPlayer = (currentPlayer + 1) % players.size();
			if(!isGameOver()){ //If game is over then no one should make any more moves
				if(!(m instanceof DoubleMove)) //Double Moves have already been taken care of
					updateSpectators(m);
                if(currentPlayer == 0){
					for(Spectator s: spectators){
						s.onRotationComplete(this);
					}
                }
                else{
					takeMove();
                }
            }else{
			    updateSpectators(m);
				for(Spectator s: spectators){
					s.onGameOver(this,winningPlayers);
				}
			}
    }
    private void updateGameOver() {
	    boolean roundsUsed = currentRound == rounds.size();
        boolean mrXStuck = validMove(BLACK).isEmpty();
        boolean endOfRot = currentPlayer == players.size() -1;
        gameOver = ((roundsUsed || areDetectivesStuck() || mrXStuck) && (endOfRot || gameNotStarted) )|| isMrXCaptured();
        if(areDetectivesStuck() || (roundsUsed && !isMrXCaptured() && endOfRot)){
        	winningPlayers.add(BLACK);
		}
		if(mrXStuck||isMrXCaptured()){
        	for(ScotlandYardPlayer p : players){
        		if(p.isDetective()){
        			winningPlayers.add(p.colour());
				}
			}
		}
    }

    private void updateSpectators(Move m){
	    ScotlandYardPlayer p = (currentPlayer != 0) ? players.get(currentPlayer - 1) : players.get(players.size() - 1);
	    Move move = (p.isMrX() && !revealRound) ? new TicketMove(m.colour(),((TicketMove) m).ticket(),xLastLocation) :  m;
	    for(Spectator s : spectators){
                if(p.isMrX() && !gameOver)
                    s.onRoundStarted(this,currentRound);// if previous player was mrX a new round has started
                s.onMoveMade(this, move);
        }
    }

    private void getIntermediateLocation(TicketMove m, boolean round) { //updates MrX's location after a move
		if (round) {
			xActualLocation = m.destination();
			xLastLocation = xActualLocation;
		}
	}

	private DoubleMove getHiddenDoubleMoves(DoubleMove m) { //creates a DoubleMove based on revealRounds
		// updates MrX's actual and last locations if the current round is a revealRound
		getIntermediateLocation(m.firstMove(), revealRound);
		boolean nextRevealRound = rounds.get(currentRound + 1);
		TicketMove firstMove = new TicketMove(m.colour(),m.firstMove().ticket(),xLastLocation);
		//updates MrX's actual and last locations if the next round is a revealRound
		getIntermediateLocation(m.secondMove(), nextRevealRound);
		TicketMove secondMove = new TicketMove(m.colour(),m.secondMove().ticket(),xLastLocation);
		return new DoubleMove(m.colour(),firstMove,secondMove);
	}

    private void updateDoubleSpec(DoubleMove m,ScotlandYardPlayer mrX){
   		int oldXlastLocation = xLastLocation;
		boolean nextRevealRound = rounds.get(currentRound + 1);
		boolean currentRevealRound = revealRound;
		boolean prevRevealRound = (currentRound < 1) ? rounds.get(0) : rounds.get(currentRound - 1);

		DoubleMove hidden = getHiddenDoubleMoves(m); //locations of m are updated based on revealRound
        Ticket ticket1 = hidden.firstMove().ticket();
        Ticket ticket2 = hidden.secondMove().ticket();

		currentPlayer += 1;
		updateRevealAndLocation(prevRevealRound,oldXlastLocation);
		mrX.removeTicket(DOUBLE);
		for(Spectator s: spectators){
			s.onMoveMade(this,hidden);
			announceMove(hidden.firstMove(),s,mrX, m.firstMove(), currentRevealRound); //are 5 arguments too many?
			announceMove(hidden.secondMove(),s,mrX, m.secondMove(), nextRevealRound);
			currentRound -= 2; //current Round is reduced to keep it looping
			//tickets are re-added to keep it looping as well
			mrX.addTicket(ticket1);
			mrX.addTicket(ticket2);
		}
		mrX.removeTicket(ticket1);
		mrX.removeTicket(ticket2);
		currentPlayer -= 1; //current Player is decremented as it's incremented outside
		currentRound += 2;
	}

   private void updateRevealAndLocation(boolean x,int loc){
	    revealRound = x;
	    xLastLocation = loc;
   }

    private void announceMove(TicketMove hidden, Spectator s,ScotlandYardPlayer mrX, TicketMove actual, boolean reveal){
        updateRevealAndLocation(reveal,hidden.destination());
        xActualLocation = actual.destination();
	    currentRound += 1;
		mrX.removeTicket(actual.ticket());
   		s.onRoundStarted(this,currentRound);
   		s.onMoveMade(this,hidden);
    }

    private boolean areDetectivesStuck() {
        boolean isStuck = true;
        for (ScotlandYardPlayer p : players) {
            if (p.isDetective()) {
				PassMove pass = new PassMove(p.colour());
                isStuck = isStuck && validMove(p.colour()).contains(pass); // if detectives are stuck, they should only have the pass move
            }
        }
        return isStuck;
    }

    private boolean isMrXCaptured() {
        for (ScotlandYardPlayer p : players) {
            if (p.isDetective() && p.location() == xActualLocation) {
                return true;
            }
        }
        return false;
    }

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		return Collections.unmodifiableList(spectators);
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
		return Collections.unmodifiableSet(winningPlayers);
	}

	// The location of a player with a given colour in its last known location.
	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		for (ScotlandYardPlayer player : players) {
		    if (player.colour() == colour) {
		        if (player.isMrX()) {
		            if (revealRound) {
		            	xLastLocation = xActualLocation;
		            }
		            return Optional.of(xLastLocation); // if MrX is hidden this round, return 0

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
		return gameOver;
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