package lk.ijse.dep.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AiPlayer extends Player {

    private static final int SIMULATION_COUNT = 2000;

    private static final double EXPLORATION_PARAM = Math.sqrt(2);

    public AiPlayer(Board board) {
        super(board);
    }

    @Override
    public void movePiece(int col) {

        int bestCol = mctsSearch();
        board.updateMove(bestCol, Piece.GREEN);
        board.getBoardUI().update(bestCol, false);

        Winner winner = board.findWinner();

        if (winner.getWinningPiece() != Piece.EMPTY) {
            board.getBoardUI().notifyWinner(winner);
        } else if (board.existLegalMoves()) {
            board.getBoardUI().notifyWinner(new Winner(Piece.EMPTY));
        }
    }

    private int mctsSearch() {
        Node root = new Node(null, -1, ((BoardImpl) board).getPieces(), 0);


        for (int i = 0; i < SIMULATION_COUNT; i++) {
            Node promisingNode = selectPromisingNode(root);

            if (promisingNode.isExpandable()) {
                promisingNode.expand();
            }

            Node nodeToExplore = promisingNode;
            if (!promisingNode.children.isEmpty()) {
                nodeToExplore = promisingNode.getRandomChild();
            }

            int playoutResult = simulatePlayout(nodeToExplore);

            backpropagate(nodeToExplore, playoutResult);
        }


        Node winnerNode = root.getChildWithMaxScore();
        return winnerNode.action;
    }

    // Static nested class for MCTS tree nodes
    private static class Node {
        Node parent;
        List<Node> children = new ArrayList<>();
        int action; // Column chosen to reach this state (-1 for root)
        Piece[][] state; // Board state
        int visits; // Number of simulations through this node
        double wins; // Cumulative wins (can be fractional for draws)
        int depth; // Depth in tree (even: AI, odd: Human)

        public Node(Node parent, int action, Piece[][] state, int depth) {
            this.parent = parent;
            this.action = action;
            this.depth = depth;
            this.state = copyBoard(state);
        }

        // Helper: Deep copy board state
        private Piece[][] copyBoard(Piece[][] original) {
            if (original == null) return null;
            Piece[][] copy = new Piece[Board.NUM_OF_COLS][Board.NUM_OF_ROWS];
            for (int c = 0; c < Board.NUM_OF_COLS; c++) {
                System.arraycopy(original[c], 0, copy[c], 0, Board.NUM_OF_ROWS);
            }
            return copy;
        }


        public void expand() {


            List<Integer> legalActions = getLegalActions(state);
            Piece playerPiece = (depth % 2 == 0) ? Piece.GREEN : Piece.BLUE; // AI even, Human odd
            for (int col : legalActions) {
                Piece[][] newState = copyBoard(state);
                int row = findNextRow(newState, col);
                newState[col][row] = playerPiece;
                children.add(new Node(this, col, newState, depth + 1));
            }
        }


        public Node getBestChild() {
            Node best = null;
            double maxScore = Double.NEGATIVE_INFINITY;
            for (Node child : children) {
                double uct = calculateUCT(child);
                if (uct > maxScore) {
                    maxScore = uct;
                    best = child;
                }
            }
            return best;
        }


        private double calculateUCT(Node child) {
            if (child.visits == 0) return Double.POSITIVE_INFINITY;
            double exploitation = child.wins / child.visits;
            double exploration = EXPLORATION_PARAM * Math.sqrt(Math.log(visits) / child.visits);
            return exploitation + exploration;
        }

        public Node getRandomChild() {
            return children.get(new Random().nextInt(children.size()));
        }

        public Node getChildWithMaxScore() {
            Node best = null;
            double maxWins = Double.NEGATIVE_INFINITY;
            for (Node child : children) {
                if (child.wins > maxWins) {
                    maxWins = child.wins;
                    best = child;
                }
            }
            return best;
        }

        public boolean isExpandable() {
            return !isTerminal(state) && children.isEmpty();
        }

        private List<Integer> getLegalActions(Piece[][] board) {
            List<Integer> actions = new ArrayList<>();
            for (int col = 0; col < Board.NUM_OF_COLS; col++) {
                if (findNextRow(board, col) != -1) {
                    actions.add(col);
                }
            }
            return actions;
        }

        private int findNextRow(Piece[][] board, int col) {
            for (int row = 0; row < Board.NUM_OF_ROWS; row++) {
                if (board[col][row] == Piece.EMPTY) {
                    return row;
                }
            }
            return -1;
        }

        private boolean isTerminal(Piece[][] board) {
            return checkWinner(board) != Piece.EMPTY || getLegalActions(board).isEmpty();
        }

        private Piece checkWinner(Piece[][] board) {
            // Horizontal
            for (int row = 0; row < Board.NUM_OF_ROWS; row++) {
                for (int col = 0; col < Board.NUM_OF_COLS - 3; col++) {
                    Piece p = board[col][row];
                    if (p != Piece.EMPTY && p == board[col + 1][row] && p == board[col + 2][row] && p == board[col + 3][row]) {
                        return p;
                    }
                }
            }
            // Vertical
            for (int col = 0; col < Board.NUM_OF_COLS; col++) {
                for (int row = 0; row < Board.NUM_OF_ROWS - 3; row++) {
                    Piece p = board[col][row];
                    if (p != Piece.EMPTY && p == board[col][row + 1] && p == board[col][row + 2] && p == board[col][row + 3]) {
                        return p;
                    }
                }
            }
            return Piece.EMPTY;
        }
    }

    // MCTS Selection: Traverse to promising leaf using UCT
    private Node selectPromisingNode(Node root) {
        Node node = root;
        while (!node.children.isEmpty()) {
            node = node.getBestChild();
        }
        return node;
    }

    private int simulatePlayout(Node node) {
        Piece[][] tempState = node.copyBoard(node.state);
        Piece currentPlayer = (node.depth % 2 == 0) ? Piece.GREEN : Piece.BLUE;
        Piece winner = node.checkWinner(tempState);

        while (winner == Piece.EMPTY && !node.getLegalActions(tempState).isEmpty()) {
            List<Integer> legalActions = node.getLegalActions(tempState);
            int col = legalActions.get(new Random().nextInt(legalActions.size()));
            int row = node.findNextRow(tempState, col);
            tempState[col][row] = currentPlayer;
            winner = node.checkWinner(tempState);
            currentPlayer = (currentPlayer == Piece.GREEN) ? Piece.BLUE : Piece.GREEN;
        }

        if (winner == Piece.GREEN) return 1;
        if (winner == Piece.BLUE) return -1;
        return 0;
    }


    private void backpropagate(Node node, int result) {
        Node temp = node;
        while (temp != null) {
            temp.visits++;
            if (temp.depth % 2 == 1) {
                temp.wins -= result;
            } else {
                temp.wins += result;
            }
            temp = temp.parent;
        }
    }
}