package org.auther.dal.mock;

import com.google.inject.Singleton;
import org.auther.dal.OtpRepository;
import org.auther.dal.model.OneTimePasswordDO;

@Singleton
public class MockOtpRepository extends AbstractRepository<OneTimePasswordDO> implements OtpRepository {
}
