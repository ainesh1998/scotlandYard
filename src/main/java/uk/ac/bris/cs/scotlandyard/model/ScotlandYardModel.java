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


// A working implementation of ScotlandYardModel. 124 tests pass.


public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor{
	private final List<Boolean> rounds;
	private final Graph<Integer,Transport> map;
	private List<ScotlandYardPlayer> players = new ArrayList<>();
	private int currentRound = 0;
	private int currentPlayer;
	private boolean gameOver = false;
	private int xLastLocation = 0; // MrX's last known location
	private int xActualLocation; // Stores MrX's actual location at a given time
	private boolean revealRound = false;
	private boolean gameNotStarted = true; // The first rotation of startRotate has not been entered yet
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
		players.add(0,new ScotlandYardPlayer(mrX.player,mrX.colour,mrX.location,mrX.tickets)); //MrX as a ScotlandYardPlayer
		players.add(1, new ScotlandYardPlayer(firstDetective.player,firstDetective.colour,firstDetective.location,firstDetective.tickets)); //firstDetective
		currentPlayer = 0; //game starts with MrX
		if(graph.isEmpty()) //map cannot be empty
			throw new IllegalArgumentException("map should not be empty");
		if(rounds.isEmpty()){ //there must be at least one round
			throw new IllegalArgumentException("Empty Rounds");
		}
		checkValidMrx(mrX);
		for(PlayerConfiguration x : restOfTheDetectives){
			checkValidDetective(x);
			players.add(new ScotlandYardPlayer(x.player,x.colour,x.location,x.tickets)); //adds ScotlandYardPlayers to list of players
		}

		checkValidDetective(firstDetective);
		checkOverlap(players);
		checkDuplicate(players);
		updateGameOver();
	}

	// MrX must have all tickets and must be black
	private void checkValidMrx(PlayerConfiguration mrX){
		if(mrX.colour != BLACK) // mrX cannot be a detective's colour
			throw new IllegalArgumentException("MrX should be Black");
		if(missingTickets(mrX))
			throw new IllegalArgumentException(("MrX is missing tickets"));
	}

	// Player start locations must not overlap
	private void checkOverlap(List<ScotlandYardPlayer> players){
		Set<Integer> locations = new HashSet<>();
		for(ScotlandYardPlayer x : players){
			if(locations.contains(x.location()))
				throw new IllegalArgumentException("2 players are in the same location");
			locations.add(x.location());
		}
	}

	// There can only be one player of each colour
	private void checkDuplicate(List<ScotlandYardPlayer> players){
		Set<Colour> locations = new HashSet<>();
		for(ScotlandYardPlayer x : players){
			if(locations.contains(x.colour()))
				throw new IllegalArgumentException("There is already a player of this colour");
			locations.add(x.colour());
		}
	}

	// Checks if the given PlayerConfiguration is not a detective
	private boolean isNotDetective(PlayerConfiguration x){ //checks whether a player is a detective or not
		return (x.colour.isMrX() || x.tickets.get(DOUBLE) != 0|| x.tickets.get(SECRET) != 0);
	}

	// Checks if the given PlayerConfiguration is missing tickets
	private boolean missingTickets(PlayerConfiguration x){
		return !(x.tickets.containsKey(TAXI) && x.tickets.containsKey(BUS) && x.tickets.containsKey(UNDERGROUND) && x.tickets.containsKey(DOUBLE) && x.tickets.containsKey(SECRET) );
	}

	// Checks if the given PlayerConfiguration is a valid detective
	private void checkValidDetective(PlayerConfiguration x){
		requireNonNull(x);
		if(missingTickets(x)) //if a detective doesn't have a ticket type it should fail
			throw new IllegalArgumentException("detective is missing tickets");
		if(isNotDetective(x)) //If there is a mrX in your list of detectives it should fail
			throw new IllegalArgumentException("Only 1 mrX is allowed.");
	}

	// Adds a spectator to the list of spectators
	@Override
	public void registerSpectator(Spectator spectator) {
		if(spectators.contains(spectator))
			throw new IllegalArgumentException("spectator already registered");
		spectators.add(requireNonNull(spectator));
	}

	// Removes a spectator from the list of spectators
	@Override
	public void unregisterSpectator(Spectator spectator) {
		requireNonNull(spectator);
		if(!spectators.contains(spectator))
			throw new IllegalArgumentException("spectator wasn't registered");
		spectators.remove(requireNonNull(spectator));
	}

	// Starts a rotation of the game, always starting with MrX
	@Override
	public void startRotate() {
		gameNotStarted = false;
		if(gameOver)
			throw new IllegalStateException();
		ScotlandYardPlayer p = players.get(0);
		p.player().makeMove(this,xActualLocation,validMove(p.colour()),this);
	}

	// When a detective makes a move
	private void takeMove(){
		ScotlandYardPlayer p = players.get(currentPlayer);
		p.player().makeMove(this,p.location(),validMove(p.colour()),this);
	}

	// Gets all valid TicketMoves
	private Set<Move> getTicketMoves(Edge<Integer,Transport> e,Set<Move> moves,Colour player){
		Ticket ticket = fromTransport(e.data());
		int destination = e.destination().value();
		//if there aren't any more rounds then double moves should not be added
        Boolean noMoreRounds = (getCurrentRound() == rounds.size() - 1 );
        //players can't move to locations occupied by detectives
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

	// Gets all valid DoubleMoves
	private Set<Move> getDoubleMoves(Edge<Integer,Transport> e,Set<Move> moves,Colour player,Ticket ticket,int destination){
		Ticket ticket2 = fromTransport(e.data());
		int destination2 = e.destination().value();
		//ensures that player has enough tickets for move
		Boolean sameTickets = (ticket.equals(ticket2) && getScotPlayer(player).tickets().get(ticket) < 2 );
		if(!getDetectiveLocations().contains(destination) && !getDetectiveLocations().contains(destination2) && getScotPlayer(player).hasTickets(ticket2)) {
            if (getScotPlayer(player).hasTickets(SECRET)) {
            	// add all possible combinations of a SECRET ticket since they can replace any other transport
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

	// Returns a set of all valid moves based on a player's location
	private Set<Move> validMove(Colour player){ //Generates all possible moves that can be made from anywhere on the board
		ScotlandYardPlayer p = getScotPlayer(player);
		Node<Integer> position = p.isMrX()? new Node<>(xActualLocation):new Node<>(p.location());
		Collection<Edge<Integer,Transport>> edges = map.getEdgesFrom(position);
		Set<Move> moves = new HashSet<>();
		for(Edge<Integer,Transport> e : edges){
			moves = getTicketMoves(e,moves,player);
		}
		// if there's no available place to move for a detective
		if(player.isDetective() && moves.size() == 0)
			moves.add(new PassMove(player)); // the player passes if they're a detective
		return moves;
	}

	// Returns a list of detective locations
	private List<Integer> getDetectiveLocations(){
		List<Integer> locations = new ArrayList<>();
		for(ScotlandYardPlayer p: players){
			if(p.isDetective())
				locations.add(p.location());
		}
		return locations;
	}

	// Returns a ScotlandYardPlayer of the given colour
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

	// Accepts a move
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
					s.onRotationComplete(this); //rotation is complete since MrX is the next player
				}
			}
			else{
				takeMove();
			}
		}else{
			updateSpectators(m);
			for(Spectator s: spectators){
				s.onGameOver(this,winningPlayers); //game has ended
			}
		}
	}

	// Updates gameOver and returns the winning players if the game is over
    private void updateGameOver() {
	    boolean roundsUsed = currentRound == rounds.size();
        boolean mrXStuck = validMove(BLACK).isEmpty();
        boolean endOfRot = currentPlayer == players.size() -1;
        /*the game is over if the rounds are used, the detectives are stuck or mrX is stuck at the end of a rotation
        or if mrX is captured at any point in the game */
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
	// Updates the spectators for moves that are not double moves
	private void updateSpectators(Move m){
		ScotlandYardPlayer p = (currentPlayer != 0) ? players.get(currentPlayer - 1) : players.get(players.size() - 1);
		Move move = (p.isMrX() && !revealRound) ? new TicketMove(m.colour(),((TicketMove) m).ticket(),xLastLocation) :  m;
		for(Spectator s : spectators){
			if(p.isMrX() && !gameOver)
				s.onRoundStarted(this,currentRound);// if previous player was mrX a new round has started
			s.onMoveMade(this, move);
		}
	}

	// Updates MrX's actual and last known locations after a move
	private void getIntermediateLocation(TicketMove m, boolean round) {
		if (round) {
			xActualLocation = m.destination();
			xLastLocation = xActualLocation;
		}
	}

	// Creates a DoubleMove based on whether the round is a revealRound or not
	private DoubleMove getHiddenDoubleMoves(DoubleMove m) {
		// updates MrX's actual and last locations if the current round is a revealRound
		getIntermediateLocation(m.firstMove(), revealRound);
		boolean nextRevealRound = rounds.get(currentRound + 1);
		TicketMove firstMove = new TicketMove(m.colour(),m.firstMove().ticket(),xLastLocation);
		//updates MrX's actual and last locations if the next round is a revealRound
		getIntermediateLocation(m.secondMove(), nextRevealRound);
		TicketMove secondMove = new TicketMove(m.colour(),m.secondMove().ticket(),xLastLocation);
		return new DoubleMove(m.colour(),firstMove,secondMove);
	}

	// Handles the Spectator updating when the move is a double move
	private void updateDoubleSpec(DoubleMove m,ScotlandYardPlayer mrX){
		int oldXlastLocation = xLastLocation; //MrX's last known location before the doublemove is made
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
			announceMove(hidden.firstMove(),s,mrX, m.firstMove(), currentRevealRound);
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

	// Updates revealRound and xLastLocation
	private void updateRevealAndLocation(boolean x,int loc){
		revealRound = x;
		xLastLocation = loc;
	}

	// Notifies the spectators about each TicketMove in the DoubleMove
	private void announceMove(TicketMove hidden, Spectator s,ScotlandYardPlayer mrX, TicketMove actual, boolean reveal){
		updateRevealAndLocation(reveal,hidden.destination());
		xActualLocation = actual.destination();
		currentRound += 1;
		mrX.removeTicket(actual.ticket());
		s.onRoundStarted(this,currentRound);
		s.onMoveMade(this,hidden);
	}

	// Returns true if the detectives are stuck
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

	// Returns true if MrX is captured
	private boolean isMrXCaptured() {
		for (ScotlandYardPlayer p : players) {
			if (p.isDetective() && p.location() == xActualLocation) {
				return true;
			}
		}
		return false;
	}

	// Returns an immutable collection of spectators
	@Override
	public Collection<Spectator> getSpectators() { return Collections.unmodifiableList(spectators); }

	// Returns an immutable list of player colours
	@Override
	public List<Colour> getPlayers() {
		List<Colour> colours = new ArrayList<>();
		for ( ScotlandYardPlayer x: players){
			colours.add(x.colour());
		}
		return Collections.unmodifiableList(colours);
	}

	// Returns an immutable list of the winning players
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

	// Returns the number of the given ticket the given player has
	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		for (ScotlandYardPlayer player : players) {
			if (player.colour() == colour) return Optional.of(player.tickets().get(ticket));
		}
		return Optional.empty();
	}

	// Returns gameOver
	@Override
	public boolean isGameOver() {
		return gameOver;
	}

	// Returns the colour of the current player
	@Override
	public Colour getCurrentPlayer() {
		return players.get(currentPlayer).colour();
	}

	// Returns the current round
	@Override
	public int getCurrentRound() {
		return currentRound;
	}

	// Returns an immutable list of rounds
	@Override
	public List<Boolean> getRounds() { return Collections.unmodifiableList(rounds); }

	// Returns an immutable graph of the Scotland Yard map
	@Override
	public Graph<Integer, Transport> getGraph() { return new ImmutableGraph<>(map); }

}
