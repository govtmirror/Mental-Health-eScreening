package gov.va.escreening.repository;

import gov.va.escreening.entity.UserProgram;

public interface UserProgramRepository extends RepositoryInterface<UserProgram> {

    boolean hasUserAndProgram(Integer userId, Integer programId);
}
