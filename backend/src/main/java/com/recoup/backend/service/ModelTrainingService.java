package com.recoup.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recoup.backend.model.RecoveryCase;
import com.recoup.backend.repository.RecoveryCaseRepository;

/** Retrains the recovery-probability model from everything the pipeline has
 *  observed so far. Called once at the start of every batch run (see
 *  BatchController), before that batch's own events are generated, so a
 *  batch never trains on its own outcomes. */
@Service
public class ModelTrainingService {

    private final RecoveryCaseRepository caseRepository;
    private final MlRecoveryProbabilityEstimator estimator;

    public ModelTrainingService(RecoveryCaseRepository caseRepository, MlRecoveryProbabilityEstimator estimator) {
        this.caseRepository = caseRepository;
        this.estimator = estimator;
    }

    @Transactional(readOnly = true)
    public void retrain() {
        List<RecoveryCase> history = caseRepository.findByDecision_InterventionIn(MlRecoveryProbabilityEstimator.trainableInterventions());
        estimator.trainOn(history);
    }
}
