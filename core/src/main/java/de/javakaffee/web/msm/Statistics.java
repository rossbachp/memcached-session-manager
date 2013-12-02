/*
 * Copyright 2010 Martin Grotzke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.javakaffee.web.msm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

/**
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public class Statistics {

    private final AtomicLong _numRequestsWithoutSession = new AtomicLong();
    private final AtomicLong _numRequestsWithTomcatFailover = new AtomicLong();
    private final AtomicLong _numRequestsWithSession = new AtomicLong();
    private final AtomicLong _numRequestsWithMemcachedFailover = new AtomicLong();
    private final AtomicLong _numRequestsWithBackupFailure = new AtomicLong();
    private final AtomicLong _numRequestsWithoutSessionAccess = new AtomicLong();
    private final AtomicLong _numRequestsWithoutAttributesAccess = new AtomicLong();
    private final AtomicLong _numRequestsWithoutSessionModification = new AtomicLong();
    private final AtomicLong _numNonStickySessionsPingFailed = new AtomicLong();
    private final AtomicLong _numNonStickySessionsReadOnlyRequest = new AtomicLong();

    private final Map<StatsType, MinMaxAvgProbe> _probes;

    private TimeUnit unit = TimeUnit.MILLISECONDS;

    private Statistics() {
        _probes = new ConcurrentHashMap<Statistics.StatsType, Statistics.MinMaxAvgProbe>();
        for( final StatsType item : StatsType.values() ) {
            _probes.put( item, new MinMaxAvgProbe() );
        }
    }

    private Statistics(final TimeUnit unit) {
        this();
        this.unit = unit;
    }

    /**
     * Creates a new (enabled) {@link Statistics} instance.
     * @return a new instance.
     */
    public static Statistics create() {
        return create( true );
    }

    /**
     * Creates a new {@link Statistics} instance which either actually gathers
     * statistics or a dummy {@link Statistics} object that discards all data.
     *
     * @param enabled specifies if stats shall be gathered or discarded.
     * @return a new {@link Statistics} instance
     */
    public static Statistics create( final boolean enabled ) {
        return enabled ? new Statistics() : DISABLED_STATS;
    }

    public static Statistics create( final boolean enabled, final TimeUnit unit ) {
        return enabled ? new Statistics(unit) : DISABLED_STATS;
    }

    /**
     * Start time measurement
     * @param statsType the specific execution type that is measured.
     * @return current timestamp to start metrics
     */
    public Watch stopWatch( @Nonnull final StatsType statsType ) {
        return new Watch( this, statsType ) ;
    }

    /**
     * A utility method that calculates the difference of the time
     * between the given <code>startInMillis</code> and {@link System#currentTimeMillis()}
     * and registers the difference via {@link #register(StatsType, long)} for the probe of the given {@link StatsType}.
     * @param statsType the specific execution type that is measured.
     * @param startInMillis the time in millis that shall be subtracted from {@link System#currentTimeMillis()}.
     * @deprecated since 1.6.6
     */
    public void registerSince( @Nonnull final StatsType statsType, final long startInMillis ) {
        register( statsType, System.currentTimeMillis() - startInMillis );
    }

    /**
     * Register the given value via {@link MinMaxAvgProbe#register(long)} for the probe of the given {@link StatsType}.
     * @param statsType the specific execution type that is measured.
     * @param value the value to register.
     */
    public void register( @Nonnull final StatsType statsType, final long value ) {
        _probes.get( statsType ).register( value );
    }

    @Nonnull
    public MinMaxAvgProbe getProbe( @Nonnull final StatsType statsType ) {
        return _probes.get( statsType );
    }

    public void requestWithoutSession() {
        _numRequestsWithoutSession.incrementAndGet();
    }
    public long getRequestsWithoutSession() {
        return _numRequestsWithoutSession.get();
    }
    public void requestWithSession() {
        _numRequestsWithSession.incrementAndGet();
    }
    public long getRequestsWithSession() {
        return _numRequestsWithSession.get();
    }
    public void requestWithTomcatFailover() {
        _numRequestsWithTomcatFailover.incrementAndGet();
    }
    public long getRequestsWithTomcatFailover() {
        return _numRequestsWithTomcatFailover.get();
    }
    public void requestWithMemcachedFailover() {
        _numRequestsWithMemcachedFailover.incrementAndGet();
    }
    public long getRequestsWithMemcachedFailover() {
        return _numRequestsWithMemcachedFailover.get();
    }
    public void requestWithBackupFailure() {
        _numRequestsWithBackupFailure.incrementAndGet();
    }
    public long getRequestsWithBackupFailure() {
        return _numRequestsWithBackupFailure.get();
    }
    public void requestWithoutSessionAccess() {
        _numRequestsWithoutSessionAccess.incrementAndGet();
    }
    public long getRequestsWithoutSessionAccess() {
        return _numRequestsWithoutSessionAccess.get();
    }
    public void requestWithoutAttributesAccess() {
        _numRequestsWithoutAttributesAccess.incrementAndGet();
    }
    public long getRequestsWithoutAttributesAccess() {
        return _numRequestsWithoutAttributesAccess.get();
    }
    public void requestWithoutSessionModification() {
        _numRequestsWithoutSessionModification.incrementAndGet();
    }
    public long getRequestsWithoutSessionModification() {
        return _numRequestsWithoutSessionModification.get();
    }

    public void nonStickySessionsPingFailed() {
        _numNonStickySessionsPingFailed.incrementAndGet();
    }
    public long getNonStickySessionsPingFailed() {
        return _numNonStickySessionsPingFailed.get();
    }

    public void nonStickySessionsReadOnlyRequest() {
        _numNonStickySessionsReadOnlyRequest.incrementAndGet();
    }
    public long getNonStickySessionsReadOnlyRequest() {
        return _numNonStickySessionsReadOnlyRequest.get();
    }

    public static enum StatsType {

        /**
         * Provides info regarding the effective time that was required for session
         * backup in the request thread and it's measured for every request with a session,
         * even if the session id has not set memcached id (this is the time that was effectively
         * required as part of the client request). It should differ from {@link #getBackupProbe()}
         * if async session backup shall be done.
         *
         * @see BackupSessionService#backupSession(MemcachedBackupSession, boolean)
         */
        EFFECTIVE_BACKUP,

        /**
         * Provides info regarding the time that was required for session backup,
         * excluding skipped backups and excluding backups where a session was relocated.
         */
        BACKUP,
        ATTRIBUTES_SERIALIZATION,
        SESSION_DESERIALIZATION,
        MEMCACHED_UPDATE,
        LOAD_FROM_MEMCACHED,
        DELETE_FROM_MEMCACHED,
        CACHED_DATA_SIZE,

        /**
         * Lock acquiration in non-sticky session mode.
         */
        ACQUIRE_LOCK,

        /**
         * Lock acquiration failures in non-sticky session mode.
         */
        ACQUIRE_LOCK_FAILURE,

        /**
         * Lock release in non-sticky session mode.
         */
        RELEASE_LOCK,

        /**
         * Time spent (in the request thread) for non-sticky sessions at the end of requests that did not access
         * the session (performs validity load/update, ping session, ping 2nd session backup, update validity backup in secondary memcached).
         */
        NON_STICKY_ON_BACKUP_WITHOUT_LOADED_SESSION,

        /**
         * Time spent for non-sticky sessions after session backup in the request thread (ping session, store validity info / meta data,
         * store additional backup in secondary memcached).
         */
        NON_STICKY_AFTER_BACKUP,

        /**
         * Tasks executed for non-sticky sessions after a session was loaded from memcached (load validity info / meta data).
         */
        NON_STICKY_AFTER_LOAD_FROM_MEMCACHED,

        /**
         * Tasks executed for non-sticky sessions after a session was deleted from memcached (delete validity info and backup data).
         */
        NON_STICKY_AFTER_DELETE_FROM_MEMCACHED

    }


    /**
     * Watch a block in time from one thread!
     */
    public class Watch {
        private final long startTime;
        private long deltaTime;
        private final StatsType statsType;
        private final Statistics statistics;
        private boolean first = true;

        public Watch(final Statistics statistics, final StatsType statsType) {
            this.statistics = statistics;
            this.statsType = statsType;
            this.startTime = System.nanoTime() ;
        }

        private long convertToUnit(long l){
            if ( unit == TimeUnit.MILLISECONDS)
                return TimeUnit.NANOSECONDS.toMillis(l);
            else if (unit == TimeUnit.MICROSECONDS)
                return TimeUnit.NANOSECONDS.toMicros(l);
            return l ;
        }

        protected long getStartTime() {
            return convertToUnit(startTime);
        }

        public StatsType getStatsType() {
            return statsType;
        }

        public long getTime() {
            return convertToUnit(deltaTime);
        }

        public TimeUnit getUnit() {
            return unit;
        }

        public void stop() {
            if(first) {
                deltaTime = System.nanoTime() - startTime;
                statistics.getProbe(statsType).register(convertToUnit(deltaTime));
                first = false;
            } else {
                throw new IllegalStateException("Can't stop the watch " + statsType + " multiple times!");
            }
        }

    }

    public static class MinMaxAvgProbe {
        private final Object _lock = new Object();

        private boolean _first = true;
        private final AtomicLong _count = new AtomicLong();
        private final AtomicLong _min = new AtomicLong();
        private final AtomicLong _max = new AtomicLong();
        private volatile double _avg ;

        /**
         * A utility method that calculates the difference of the time
         * between the given <code>startInMillis</code> and {@link System#currentTimeMillis()}
         * and registers the difference via {@link #register(long)}.
         * @param startInMillis the time in millis that shall be subtracted from {@link System#currentTimeMillis()}.
         */
        public void registerSince( final long startInMillis ) {
            register( System.currentTimeMillis() - startInMillis );
        }

        /**
         * Register the given value.
         * @param value the value to register.
         */
        public void register( final long value ) {
            synchronized (_lock) {
                if ( value < _min.get() || _first ) {
                    _min.set(value);
                }
                if ( value > _max.get() || _first ) {
                    _max.set(value);
                }
                _avg = ( _avg * _count.get() + value ) / _count.incrementAndGet();
                _first = false;
            }
        }

        /**
         * @return the count
         */
        long getCount() {
            return _count.get();
        }

        /**
         * @return the min
         */
        long getMin() {
            return _min.get();
        }

        /**
         * @return the max
         */
        long getMax() {
            return _max.get();
        }

        /**
         * @return the avg
         */
        double getAvg() {
            return _avg;
        }

        /**
         * Returns a string array with labels and values of count, min, avg and max.
         * @return a String array.
         */
        public String[] getInfo() {
            synchronized (_lock) {
                return new String[] {
                        "Count = " + _count.get(),
                        "Min = "+ _min.get(),
                        "Avg = "+ _avg,
                        "Max = "+ _max.get()
                };
            }
        }

    }

    private static final Statistics DISABLED_STATS = new Statistics() {

        @Override
        public void registerSince(final StatsType statsType, final long startInMillis) {};

        @Override
        public void register(final StatsType statsType, final long startInMillis) {};

        public MinMaxAvgProbe getProbe( @Nonnull final StatsType statsType ) {
            return new MinMaxAvgProbe();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void requestWithBackupFailure() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void requestWithoutSession() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void requestWithoutSessionAccess() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void requestWithoutSessionModification() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void requestWithSession() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void requestWithMemcachedFailover() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void requestWithTomcatFailover() {
        }

        @Override
        public void nonStickySessionsPingFailed() {
        }

        @Override
        public void nonStickySessionsReadOnlyRequest() {
        }

        @Override
        public void requestWithoutAttributesAccess() {
        }

    };

}
