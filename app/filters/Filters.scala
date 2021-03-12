package filters

import javax.inject.Inject
import play.api.http.{DefaultHttpFilters, EnabledFilters}
import play.filters.gzip.GzipFilter

class Filters @Inject()(
                         defaultFilters: EnabledFilters,
                         gzip: GzipFilter,
                         log: LoggingFilter
                       ) extends DefaultHttpFilters(defaultFilters.filters :+ gzip :+ log: _*)
