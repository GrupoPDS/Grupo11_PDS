package br.ufu.pds.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita o mecanismo de agendamento do Spring (@Scheduled). Sem esta configuração, jobs como
 * OverdueLoanScheduler não executam.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
