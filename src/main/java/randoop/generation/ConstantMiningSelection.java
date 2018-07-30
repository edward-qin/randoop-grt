package randoop.generation;

import java.util.HashMap;
import java.util.Map;
import randoop.main.GenInputsAbstract;
import randoop.sequence.Sequence;
import randoop.util.Randomness;
import randoop.util.SimpleList;

/**
 * Implements the GRT Constant Mining component, as described by the paper "GRT:
 * Program-Analysis-Guided Random Testing" by Ma et. al (appears in ASE 2015):
 * https://people.kth.se/~artho/papers/lei-ase2015.pdf .
 */
public class ConstantMiningSelection implements InputSequenceSelector {
  /**
   * Map of extracted literal sequences to their static weights. These weights are never changed
   * once initialized.
   */
  private final Map<Sequence, Double> literalWeightMap = new HashMap<>();

  /**
   * Initialize GRT Constant Mining selection by computing weights for literals that appear in
   * classes under test.
   *
   * @param componentManager component generator from {@link ForwardGenerator} used for getting the
   *     frequency of a literal
   * @param numClasses number of classes under test
   * @param literalTermFrequencies a map from a literal to the number of times it appears in any
   *     class under test
   */
  public ConstantMiningSelection(
      ComponentManager componentManager,
      int numClasses,
      Map<Sequence, Integer> literalTermFrequencies) {
    Map<Sequence, Integer> literalDocumentFrequencies =
        componentManager.getLiteralDocumentFrequency();

    if (GenInputsAbstract.constant_mining_logging) {
      System.out.println("Literal term frequencies: ");
      System.out.println(literalTermFrequencies);
      System.out.println("Document term frequencies: ");
      System.out.println(literalDocumentFrequencies);
    }

    // We iterate through all literals that were found by the ClassLiteralExtractor.
    for (Sequence sequence : literalDocumentFrequencies.keySet()) {
      Integer documentFrequency = literalDocumentFrequencies.get(sequence);
      Integer termFrequency = literalTermFrequencies.get(sequence);

      // Compute the term frequency-inverse document frequency for GRT Constant Mining.
      double tfIdf =
          termFrequency * Math.log((numClasses + 1.0) / ((numClasses + 1.0) - documentFrequency));
      literalWeightMap.put(sequence, tfIdf);
    }
  }

  /**
   * Select a sequence from the candidate list. The candidate list contains various input sequences
   * that produce a specific type.
   *
   * <p>The {@code literalWeightMap} is concerned only with input sequences that are literals
   * extracted from the classes under test. Other input sequences that are generated by Randoop
   * throughout the test generation process will not have a corresponding weight in the
   * literalWeightMap; instead, we use an arbitrary value of 1.
   *
   * @param candidates sequences to choose from
   * @return the chosen sequence
   */
  @Override
  public Sequence selectInputSequence(SimpleList<Sequence> candidates) {
    outputCandidateWeights(candidates);

    // Iterate through the candidate list and assign any sequences with no weight a weight of 1.
    for (int i = 0; i < candidates.size(); i++) {
      Sequence candidate = candidates.get(i);
      if (!literalWeightMap.containsKey(candidate)) {
        literalWeightMap.put(candidate, 1.0);
      }
    }

    return Randomness.randomMemberWeighted(candidates, literalWeightMap);
  }

  /**
   * For debugging purposes, output the weights of the candidate list of input sequences.
   *
   * @param candidates list of input sequences
   */
  private void outputCandidateWeights(SimpleList<Sequence> candidates) {
    if (GenInputsAbstract.constant_mining_logging) {
      for (int i = 0; i < candidates.size(); i++) {
        Sequence sequence = candidates.get(i);
        Double sequenceWeight = literalWeightMap.get(sequence);
        if (sequenceWeight != null) {
          System.out.println("weight of " + sequence + " is " + sequenceWeight);
        }
      }
      System.out.println("------------");
    }
  }
}
