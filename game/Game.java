package game;
import pieces.*;
import java.util.Scanner;
import java.util.List;
public class Game{
    private BitboardBoard board;
    private String currentPlayer;
    private int turn;
    private boolean isGameRunning;
    private Scanner sc;
    public Game() throws Exception
    {
        board = new BitboardBoard();
        board.initializeBoard();
        currentPlayer = "white";
        isGameRunning = true;
        sc = new Scanner(System.in);
        turn = 0;
    }
    public void start()
    {
        System.out.println("Have a great match ahead !!! (Bitboard Engine)");
        while(isGameRunning)
        {
            board.printBoard();
            playTurn();
            if(!isGameRunning)break;
            changeTurn();
        }
    }
    public void playTurn()
    {
        while(true)
        {
            System.out.println("Its "+currentPlayer+" turn : ");
            String input = getMoveInput();
            if(isGameEnded(input))
            {
                System.out.println("Game has ended. Thank you for playing !!!");
                isGameRunning = false;
                return;
            }
            if (isUndo(input)) {
                boolean successfulUndo = board.undoLastFullMove();
                if (!successfulUndo) {
                    System.out.println("Cannot Undo.");
                }
                continue;
            }
            if(!isValidFormat(input))
            {
                System.out.println("Invalid format. Expected: <file><rank> <file><rank> (e.g. \"e2 e4\")");
                continue;
            }
            int[][] move = parseMove(input);
            int[] from = move[0];
            int[] to = move[1];
            int fromSq = from[0] * 8 + from[1];
            int toSq = to[0] * 8 + to[1];
            if(!board.isInsideBoard(from[0],from[1]))
            {
                System.out.println("Source position in not in the board");
                continue;
            }
            if(!board.isInsideBoard(to[0],to[1]))
            {
                System.out.println("Destination position is not in the board");
                continue;
            }
            Piece fromPiece = board.getPiece(from[0],from[1]);
            if(fromPiece == null)
            {
                System.out.println("There is not piece at your position");
                continue;
            }
            if(!fromPiece.color.equals(currentPlayer))
            {
                System.out.println("That is not your piece !!!");
                continue;
            }
            List<BitMove> legalMoves = board.generateLegalMoves(currentPlayer);
            boolean isValid = false;
            for(BitMove m : legalMoves){
                if(m.from() == fromSq && m.to() == toSq){
                    isValid = true;
                    break;
                }
            }
            if(!isValid)
            {
                System.out.println("Invalid " + fromPiece.getClass().getSimpleName() + " move.");
                continue;
            }
            int promotion = -1;
            if(fromPiece instanceof Pawn)
            {
                int promotionRow = currentPlayer.equals("white") ? 7 : 0;
                if(to[0] == promotionRow)
                {
                    promotion = promotePawnChoice();
                }
            }
            board.makeMove(fromSq, toSq, promotion, currentPlayer);
            String opp = (currentPlayer.equals("white")) ? "black" : "white";
            if(board.isKingInCheck(opp))
            {
                if(board.isCheckmate(opp))
                {
                    board.printBoard();
                    System.out.println("CHECKMATE : " + currentPlayer + " wins");
                    isGameRunning = false;
                    return;
                }
                else
                {
                    System.out.println("CHECK!");
                }
            }
            else if(board.isStalemate(opp))
            {
                board.printBoard();
                System.out.println("STALEMATE! The game is a draw.");
                isGameRunning = false;
                return;
            }
            break;
        }
    }
    public boolean isUndo(String input)
    {
        if(input.equalsIgnoreCase("Undo") || input.equalsIgnoreCase("U"))return true;
        return false;
    }
    public boolean isGameEnded(String input)
    {
        if(input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit"))
        {
            return true;
        }
        return false;
    }
    public boolean isValidFormat(String input)
    {
        String arr[] = input.trim().split(" ");
        if(arr.length != 2)return false;
        String from = arr[0];
        String to = arr[1];
        if(from.length() != 2)return false;
        if(to.length() != 2)return false;
        for(int i = 0 ; i < 2 ; i++)
        {
            char first = arr[i].charAt(0);
            char second = arr[i].charAt(1);
            if(first < 'a' || first > 'h')return false;
            if(second < '1' || second > '8')return false;
        }
        return true;
    }
    public String getMoveInput()
    {
        while (true)
        {
            String movement = sc.nextLine().trim();
            if (!movement.isEmpty())
            {
                return movement;
            }
            System.out.println("Enter a move.");
        }
    }
    public void changeTurn()
    {
        turn = (turn+1)%2;
        if(turn == 0)
        {
            currentPlayer = "white";
        }
        else
        {
            currentPlayer = "black";
        }
    }
    public int[][] parseMove(String movement)
    {
        String arr[] = movement.trim().split(" ");
        int from[] = parsePosition(arr[0]);
        int to[] = parsePosition(arr[1]);
        return new int[][]{from,to};
    }
    public int[] parsePosition(String move)
    {
        char column = move.charAt(0);
        char row = move.charAt(1);

        int columnIndex = column - 'a';
        int rowIndex = row - '1';

        return new int[]{rowIndex, columnIndex};
    }
    public int promotePawnChoice()
    {
        System.out.println("Pawn promotion! Choose: Queen (Q), Rook (R), Bishop (B), or Knight (N)");
        while(true)
        {
            String choice = sc.nextLine().trim().toUpperCase();
            switch(choice)
            {
                case "Q":
                    System.out.println("Promoted to Queen");
                    return BitboardBoard.QUEEN;
                case "R":
                    System.out.println("Promoted to Rook");
                    return BitboardBoard.ROOK;
                case "B":
                    System.out.println("Promoted to Bishop");
                    return BitboardBoard.BISHOP;
                case "N":
                    System.out.println("Promoted to Knight");
                    return BitboardBoard.KNIGHT;
                default:
                    System.out.println("Invalid choice. Enter Q, R, B, or N.");
                    continue;
            }
        }
    }
}
