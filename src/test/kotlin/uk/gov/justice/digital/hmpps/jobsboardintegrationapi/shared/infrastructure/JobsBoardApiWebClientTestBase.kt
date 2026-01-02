package uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.infrastructure

import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.employers.domain.Employer
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.jobs.domain.Job
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.application.UnitTestBase
import uk.gov.justice.digital.hmpps.jobsboardintegrationapi.shared.domain.JobsBoardApiClient.Companion.FETCH_SIZE
import java.net.URI
import java.util.function.Function

abstract class JobsBoardApiWebClientTestBase : UnitTestBase() {
  @Mock
  protected lateinit var webClient: WebClient

  @InjectMocks
  protected lateinit var jobsBoardApiWebClient: JobsBoardApiWebClient

  val requestUriMock = mock(WebClient.RequestHeadersUriSpec::class.java)
  val requestHeadersMock = mock(WebClient.RequestHeadersSpec::class.java)
  val responseSpecMock = mock(WebClient.ResponseSpec::class.java)

  private fun onRequest() {
    whenever(webClient.get()).thenReturn(requestUriMock)
    whenever(requestHeadersMock.accept(APPLICATION_JSON)).thenReturn(requestHeadersMock)
    whenever(requestHeadersMock.retrieve()).thenReturn(responseSpecMock)
  }

  private fun onRequestById(uri: String, id: String) {
    onRequest()
    whenever(requestUriMock.uri(uri, id)).thenReturn(requestHeadersMock)
  }

  protected fun <T> replyOnRequestById(elementClass: Class<T & Any>, response: T & Any, uri: String, id: String) {
    onRequestById(uri, id)
    whenever(responseSpecMock.bodyToMono(elementClass)).thenReturn(Mono.just(response))
  }

  protected fun <T> replyOnPagedRequest(typeReference: ParameterizedTypeReference<T & Any>, response: T & Any, uri: String, page: Int = 0, pageSize: Int = FETCH_SIZE) {
    onRequest()
    whenever(requestUriMock.uri(any<Function<UriBuilder, URI>>())).thenReturn(requestHeadersMock)
    whenever(responseSpecMock.bodyToMono(typeReference)).thenReturn(Mono.just(response))
  }

  protected fun Employer.makeCopy() = copy(createdAt = defaultCurrentTime)
  protected fun Job.makeCopy() = let { it.copy(createdAt = defaultCurrentTime).apply { employer = it.employer } }
}
