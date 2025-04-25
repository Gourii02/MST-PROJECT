import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

class Edge implements Comparable<Edge> {
    String source;
    String destination;
    int weight;

    public Edge(String source, String destination, int weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    @Override
    public int compareTo(Edge other) {
        return Integer.compare(this.weight, other.weight);
    }

    @Override
    public String toString() {
        return "(" + source + " -- " + destination + ", weight: " + weight + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Edge other = (Edge) obj;
        return (this.source.equals(other.source) && this.destination.equals(other.destination) && this.weight == other.weight) ||
               (this.source.equals(other.destination) && this.destination.equals(other.source) && this.weight == other.weight);
    }

    @Override
    public int hashCode() {
        String s1 = source.compareTo(destination) < 0 ? source : destination;
        String s2 = source.compareTo(destination) < 0 ? destination : source;
        return Objects.hash(s1, s2, weight);
    }
}

// Corrected DSU class with path compression and union-by-rank
class DSU {
    private Map<String, String> parent;
    private Map<String, Integer> rank;

    public DSU(Set<String> nodes) {
        parent = new HashMap<>();
        rank = new HashMap<>();
        for (String node : nodes) {
            parent.put(node, node);
            rank.put(node, 0);
        }
    }

    public String find(String i) {
        if (!parent.get(i).equals(i)) {
            parent.put(i, find(parent.get(i))); // Path compression
        }
        return parent.get(i);
    }

    public void union(String x, String y) {
        String rootX = find(x);
        String rootY = find(y);
        
        if (rootX.equals(rootY)) return;

        if (rank.get(rootX) < rank.get(rootY)) {
            parent.put(rootX, rootY);
        } else {
            parent.put(rootY, rootX);
            if (rank.get(rootX).equals(rank.get(rootY))) {
                rank.put(rootX, rank.get(rootX) + 1);
            }
        }
    }
}

public class MSTGUI extends JFrame implements ActionListener {

    private JPanel graphPanel;
    private JComboBox<String> algorithmChooser;
    private JLabel startNodeLabel;
    private JTextField startNodeField;
    private JButton findMSTButton;
    private JButton nextStepButton;
    private JTextArea resultArea;

    private List<Edge> allEdges;
    private Set<String> allNodes;
    private List<List<Edge>> kruskalSteps;
    private List<List<Edge>> primSteps;
    private int currentStep = 0;
    private String currentAlgorithm = "";
    private String primStartNode = "";

    // Define node positions based on Figure 1.1 (estimated coordinates)
    private final Map<String, Point> nodePositions = new HashMap<>() {{
        put("A", new Point(300, 50));
        put("B", new Point(100, 100));
        put("C", new Point(100, 250));
        put("D", new Point(200, 350));
        put("E", new Point(350, 350));
        put("F", new Point(350, 200));
        put("G", new Point(500, 250));
        put("H", new Point(500, 100));
    }};

    public MSTGUI() {
        setTitle("MST Algorithm Visualization");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 600);
        setLayout(new BorderLayout());

        // Initialize graph data with the corrected edges
        allNodes = new HashSet<>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H"));
        allEdges = Arrays.asList(
                new Edge("A", "B", 14),
                new Edge("A", "G", 8),
                new Edge("A", "F", 21),
                new Edge("B", "H", 26),
                new Edge("B", "F", 13),
                new Edge("B", "D", 14),
                new Edge("B", "C", 15),
                new Edge("C", "D", 12),
                new Edge("C", "G", 33),
                new Edge("D", "F", 12),
                new Edge("D", "E", 10),
                new Edge("E", "F", 10),
                new Edge("E", "G", 14),
                new Edge("G", "H", 7)
        );
        kruskalSteps = new ArrayList<>();
        primSteps = new ArrayList<>();

        // GUI components
        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGraph(g, getCurrentMSTEdges());
            }
        };
        graphPanel.setPreferredSize(new Dimension(600, 400));

        JPanel controlPanel = new JPanel();
        algorithmChooser = new JComboBox<>(new String[]{"Kruskal's", "Prim's"});
        startNodeLabel = new JLabel("Start Node:");
        startNodeField = new JTextField(5);
        findMSTButton = new JButton("Start Algorithm");
        findMSTButton.addActionListener(this);
        nextStepButton = new JButton("Next Step");
        nextStepButton.addActionListener(this);
        nextStepButton.setEnabled(false);
        resultArea = new JTextArea(10, 60);
        resultArea.setEditable(false);

        controlPanel.add(new JLabel("Algorithm:"));
        controlPanel.add(algorithmChooser);
        controlPanel.add(startNodeLabel);
        controlPanel.add(startNodeField);
        controlPanel.add(findMSTButton);
        controlPanel.add(nextStepButton);

        add(graphPanel, BorderLayout.CENTER);
        add(new JScrollPane(resultArea), BorderLayout.SOUTH);
        add(controlPanel, BorderLayout.NORTH);

        // Initially hide start node elements for Kruskal's
        startNodeLabel.setVisible(false);
        startNodeField.setVisible(false);
        algorithmChooser.addActionListener(e -> {
            String selectedAlgorithm = (String) algorithmChooser.getSelectedItem();
            startNodeLabel.setVisible(selectedAlgorithm.equals("Prim's"));
            startNodeField.setVisible(selectedAlgorithm.equals("Prim's"));
            if (allNodes.iterator().hasNext() && selectedAlgorithm.equals("Prim's")) {
                startNodeField.setText(allNodes.iterator().next()); // Default start node
            }
        });
        if (allNodes.iterator().hasNext()) {
            startNodeField.setText(allNodes.iterator().next()); // Set initial default start node
        }

        setVisible(true);
    }

    private void drawGraph(Graphics g, List<Edge> currentMST) {
        g.setColor(Color.BLACK);
        // Draw all original edges
        for (Edge edge : allEdges) {
            Point p1 = nodePositions.get(edge.source);
            Point p2 = nodePositions.get(edge.destination);
            if (p1 != null && p2 != null) {
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
                int midX = (p1.x + p2.x) / 2;
                int midY = (p1.y + p2.y) / 2;
                g.drawString(String.valueOf(edge.weight), midX + 5, midY - 5);
            }
        }

        // Highlight current MST edges
        g.setColor(Color.RED);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(3));
        for (Edge edge : currentMST) {
            Point p1 = nodePositions.get(edge.source);
            Point p2 = nodePositions.get(edge.destination);
            if (p1 != null && p2 != null) {
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
        g2d.setStroke(new BasicStroke(1));

        // Draw nodes
        int nodeRadius = 15;
        g.setColor(Color.BLUE);
        for (Map.Entry<String, Point> entry : nodePositions.entrySet()) {
            Point p = entry.getValue();
            g.fillOval(p.x - nodeRadius, p.y - nodeRadius, 2 * nodeRadius, 2 * nodeRadius);
            g.setColor(Color.WHITE);
            g.drawString(entry.getKey(), p.x - 5, p.y + 5);
            g.setColor(Color.BLUE);
        }
    }

    private List<Edge> getCurrentMSTEdges() {
        if (currentAlgorithm.equals("Kruskal's") && currentStep < kruskalSteps.size()) {
            return kruskalSteps.get(currentStep);
        } else if (currentAlgorithm.equals("Prim's") && currentStep < primSteps.size()) {
            return primSteps.get(currentStep);
        }
        return new ArrayList<>();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == findMSTButton) {
            currentAlgorithm = (String) algorithmChooser.getSelectedItem();
            primStartNode = startNodeField.getText().trim().toUpperCase();
            resultArea.setText("");
            kruskalSteps.clear();
            primSteps.clear();
            currentStep = 0;
            nextStepButton.setEnabled(true);
            findMSTButton.setEnabled(false);

            if (currentAlgorithm.equals("Kruskal's")) {
                resultArea.append("Running Kruskal's Algorithm...\n");
                kruskalMSTSteps(new ArrayList<>(allEdges), allNodes);
                if (kruskalSteps.isEmpty()) {
                    resultArea.append("Could not find MST using Kruskal's.\n");
                    nextStepButton.setEnabled(false);
                    findMSTButton.setEnabled(true);
                }
            } else if (currentAlgorithm.equals("Prim's")) {
                if (allNodes.contains(primStartNode)) {
                    resultArea.append("Running Prim's Algorithm starting from " + primStartNode + "...\n");
                    primMSTSteps(new ArrayList<>(allEdges), allNodes, primStartNode);
                    if (primSteps.isEmpty()) {
                        resultArea.append("Could not find MST using Prim's.\n");
                        nextStepButton.setEnabled(false);
                        findMSTButton.setEnabled(true);
                    }
                } else {
                    resultArea.append("Invalid start node: " + primStartNode + "\n");
                    nextStepButton.setEnabled(false);
                    findMSTButton.setEnabled(true);
                }
            }
            graphPanel.repaint();
        } else if (e.getSource() == nextStepButton) {
            if (currentAlgorithm.equals("Kruskal's")) {
                if (currentStep < kruskalSteps.size() - 1) {
                    currentStep++;
                    resultArea.append("Step " + (currentStep + 1) + ": Added edge " + kruskalSteps.get(currentStep).getLast() + "\n");
                } else {
                    resultArea.append("Kruskal's Algorithm finished.\n");
                    nextStepButton.setEnabled(false);
                    findMSTButton.setEnabled(true);
                }
            } else if (currentAlgorithm.equals("Prim's")) {
                if (currentStep < primSteps.size() - 1) {
                    currentStep++;
                    resultArea.append("Step " + (currentStep + 1) + ": Added edge " + primSteps.get(currentStep).getLast() + "\n");
                } else {
                    resultArea.append("Prim's Algorithm finished.\n");
                    nextStepButton.setEnabled(false);
                    findMSTButton.setEnabled(true);
                }
            }
            graphPanel.repaint();
        }
    }

    private void kruskalMSTSteps(List<Edge> edges, Set<String> nodes) {
        List<Edge> currentMST = new ArrayList<>();
        Collections.sort(edges);
        DSU dsu = new DSU(nodes);
        kruskalSteps.add(new ArrayList<>(currentMST)); // Initial state

        for (Edge edge : edges) {
            String rootSource = dsu.find(edge.source);
            String rootDestination = dsu.find(edge.destination);
            if (!rootSource.equals(rootDestination)) {
                currentMST.add(edge);
                dsu.union(edge.source, edge.destination);
                kruskalSteps.add(new ArrayList<>(currentMST)); // State after adding edge
                if (currentMST.size() == nodes.size() - 1) {
                    break;
                }
            }
        }
    }

    private void primMSTSteps(List<Edge> edges, Set<String> nodes, String startNode) {
        List<Edge> currentMST = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        PriorityQueue<Edge> pq = new PriorityQueue<>(Comparator.comparingInt(e -> e.weight));
        primSteps.add(new ArrayList<>(currentMST)); // Initial state

        visited.add(startNode);

        for (Edge edge : edges) {
            if (edge.source.equals(startNode) && !visited.contains(edge.destination)) {
                pq.offer(edge);
            } else if (edge.destination.equals(startNode) && !visited.contains(edge.source)) {
                pq.offer(new Edge(edge.destination, edge.source, edge.weight));
            }
        }

        while (!pq.isEmpty() && visited.size() < nodes.size()) {
            Edge currentEdge = pq.poll();
            String u = currentEdge.source;
            String v = currentEdge.destination;

            String nextNode = null;
            if (visited.contains(u) && !visited.contains(v)) {
                nextNode = v;
            } else if (!visited.contains(u) && visited.contains(v)) {
                nextNode = u;
                currentEdge = new Edge(v, u, currentEdge.weight);
            }

            if (nextNode != null) {
                currentMST.add(currentEdge);
                visited.add(nextNode);
                primSteps.add(new ArrayList<>(currentMST)); // State after adding edge

                for (Edge edge : edges) {
                    if (edge.source.equals(nextNode) && !visited.contains(edge.destination)) {
                        pq.offer(edge);
                    } else if (edge.destination.equals(nextNode) && !visited.contains(edge.source)) {
                        pq.offer(new Edge(edge.destination, edge.source, edge.weight));
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MSTGUI::new);
    }
}
