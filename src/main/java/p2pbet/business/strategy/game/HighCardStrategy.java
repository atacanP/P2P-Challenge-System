package p2pbet.business.strategy.game;
import p2pbet.core.entities.Participant;
import org.springframework.stereotype.Component;
import p2pbet.core.entities.GameType;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
@Component
public class HighCardStrategy implements IGameStrategy {
    private final Random random = new Random();
    private record Card(int value, int suit) implements Comparable<Card> {
        @Override
        public int compareTo(Card other) {
            if (this.value != other.value) {
                return Integer.compare(this.value, other.value);
            }
            return Integer.compare(this.suit, other.suit);
        }
        @Override
        public String toString() {
            String valueStr = switch(value) {
                case 11 -> "Jack";
                case 12 -> "Queen";
                case 13 -> "King";
                case 14 -> "Ace";
                default -> String.valueOf(value);
            };
            String suitStr = switch(suit) {
                case 4 -> "Spades";
                case 3 -> "Hearts";
                case 2 -> "Diamonds";
                case 1 -> "Clubs";
                default -> "Unknown";
            };
            return valueStr + " of " + suitStr;
        }
    }
    @Override
    public Participant determineWinner(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return null;
        }
        Participant winningParticipant = null;
        Card highestCard = null;
        Set<Card> drawnCards = new HashSet<>();
        for (Participant p : participants) {
            Card drawnCard;
            do {
                int value = random.nextInt(13) + 2; // 2 to 14
                int suit = random.nextInt(4) + 1; // 1 to 4
                drawnCard = new Card(value, suit);
            } while (drawnCards.contains(drawnCard));
            drawnCards.add(drawnCard);
            System.out.println("HighCardStrategy: Participant " + p.getUserId() + " drew " + drawnCard);
            if (highestCard == null || drawnCard.compareTo(highestCard) > 0) {
                highestCard = drawnCard;
                winningParticipant = p;
            }
        }
        System.out.println("HighCardStrategy: Winner is " + winningParticipant.getUserId() + " with " + highestCard);
        return winningParticipant; // We will never return null if there are participants
    }
    @Override
    public GameType getSupportedGameType() {
        return GameType.HIGH_CARD;
    }
}
