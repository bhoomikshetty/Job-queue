package com.bhoomik.jobqueue.scheduler;

import com.bhoomik.jobqueue.domain.Job;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class RedisService {

    final RedisTemplate<String, String> redisTemplate;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean addJobToRedis(Job job) {
        double score;
        if (job.getNextRetryAtInMs() != null) {
            score = job.getNextRetryAtInMs().toInstant().toEpochMilli();
        } else {
            score = (job.getScheduledAtInMs() == null ? job.getCreatedAtInMs() : job.getScheduledAtInMs())
                    .toInstant().toEpochMilli();
        }
        return redisTemplate.opsForZSet().add("job_queue", job.getId().toString(), score);
    }

    public List<String> getJobsFromRedis() {
        Set<String> jobIds = redisTemplate.opsForZSet().rangeByScore("job_queue", 0, System.currentTimeMillis());
        return jobIds != null ? jobIds.stream().toList() : List.of();
    }

    public Long removeJobFromRedis(Long jobId) {
        try {
            return redisTemplate.opsForZSet().remove("job_queue", jobId.toString());
        } catch (Exception e) {
            System.out.println("Error removing job from Redis for Job ID " + jobId + ": " + e.getMessage());
            return 0L;
        }
    }
}
